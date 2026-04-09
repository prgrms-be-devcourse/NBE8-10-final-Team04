from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib import error, request


@dataclass(frozen=True)
class SelectedSkill:
    skill_id: int
    category: str
    source_repo: str
    final_score: float


@dataclass(frozen=True)
class WrittenFile:
    path: str
    length: int
    sha256: str


class RunnerError(RuntimeError):
    pass


def _post_json(*, base_url: str, path: str, bearer_token: str, payload: dict[str, Any], timeout_seconds: float) -> dict[str, Any]:
    url = f"{base_url.rstrip('/')}{path}"
    body = json.dumps(payload).encode("utf-8")

    req = request.Request(
        url=url,
        method="POST",
        data=body,
        headers={
            "Authorization": f"Bearer {bearer_token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
    )

    try:
        with request.urlopen(req, timeout=timeout_seconds) as response:
            raw = response.read().decode("utf-8")
            parsed = json.loads(raw) if raw else {}
            if not isinstance(parsed, dict):
                raise RunnerError(f"{path} response must be a JSON object.")
            return parsed
    except error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RunnerError(f"HTTP {exc.code} from {path}: {body}") from exc
    except error.URLError as exc:
        raise RunnerError(f"Connection failed for {path}: {exc.reason}") from exc


def _extract_payload(response: dict[str, Any]) -> dict[str, Any]:
    payload = response.get("data", response)
    if not isinstance(payload, dict):
        raise RunnerError("API response payload must be a JSON object.")
    return payload


def _normalize_queries(raw_queries_json: str) -> list[str]:
    try:
        parsed = json.loads(raw_queries_json)
    except json.JSONDecodeError as exc:
        raise RunnerError("queries-json must be a valid JSON array of strings.") from exc

    if not isinstance(parsed, list):
        raise RunnerError("queries-json must be a JSON array of strings.")

    normalized_queries: list[str] = []
    for query in parsed:
        if not isinstance(query, str):
            raise RunnerError("queries-json must contain only strings.")
        normalized = " ".join(query.split())
        if not normalized:
            raise RunnerError("queries-json must not contain blank values.")
        normalized_queries.append(normalized)

    if not normalized_queries:
        raise RunnerError("queries-json must contain at least one query.")

    return normalized_queries


def _parse_selected_skills(response: dict[str, Any]) -> list[SelectedSkill]:
    payload = _extract_payload(response)
    raw_selected = payload.get("selectedSkills")
    if not isinstance(raw_selected, list) or not raw_selected:
        raise RunnerError("selectedSkills is missing or empty in recommendation response.")

    skills: list[SelectedSkill] = []
    for raw in raw_selected:
        if not isinstance(raw, dict):
            raise RunnerError("selectedSkills item must be a JSON object.")

        raw_skill_id = raw.get("skillId")
        skill_id = _parse_skill_id(raw_skill_id)
        if skill_id <= 0:
            raise RunnerError("skillId is missing or invalid in selectedSkills item.")

        category = str(raw.get("category", "unknown")).strip() or "unknown"
        source_repo = str(raw.get("sourceRepo", "unknown")).strip() or "unknown"
        try:
            final_score = float(raw.get("finalScore", 0.0))
        except (TypeError, ValueError):
            final_score = 0.0

        skills.append(
            SelectedSkill(
                skill_id=skill_id,
                category=category,
                source_repo=source_repo,
                final_score=final_score,
            )
        )

    return skills


def _parse_skill_id(raw_skill_id: Any) -> int:
    if isinstance(raw_skill_id, bool):
        return 0

    if isinstance(raw_skill_id, int):
        return raw_skill_id

    if isinstance(raw_skill_id, float) and raw_skill_id.is_integer():
        return int(raw_skill_id)

    if isinstance(raw_skill_id, str):
        normalized = raw_skill_id.strip()
        if normalized.lstrip("-").isdigit():
            return int(normalized)

    return 0


def _extract_skill_content(response: dict[str, Any]) -> tuple[str, str, str]:
    payload = _extract_payload(response)

    category = str(payload.get("category", "unknown")).strip() or "unknown"
    source_repo = str(payload.get("sourceRepo", "unknown")).strip() or "unknown"
    raw_markdown = payload.get("skillMdRaw")
    if not isinstance(raw_markdown, str) or not raw_markdown.strip():
        raise RunnerError("skillMdRaw is missing in skill content response.")

    return category, source_repo, raw_markdown


def _slug(raw_value: str) -> str:
    normalized = re.sub(r"[^a-zA-Z0-9_-]+", "-", raw_value.strip().lower())
    normalized = normalized.strip("-")
    return normalized or "unknown"


def _build_skill_markdown(*, skill_id: int, category: str, source_repo: str, raw_content: str) -> str:
    return (
        f"# Skill: {category} ({skill_id})\\n\\n"
        f"- category: {category}\\n"
        f"- skillId: {skill_id}\\n"
        f"- sourceRepo: {source_repo}\\n\\n"
        "## Skill Markdown\\n"
        f"{raw_content}"
    )


def _build_agents_markdown(*, selected_skills: list[SelectedSkill], queries: list[str], skill_paths: list[str]) -> str:
    lines = [
        "# AGENTS Routing",
        "",
        "## Generation Context",
        "- mode: DIRECT_RUNNER",
        f"- queries: {', '.join(queries)}",
        "",
        "## Skill Inventory",
    ]

    for selected_skill, skill_path in zip(selected_skills, skill_paths, strict=True):
        lines.append(
            f"- {skill_path} (category={selected_skill.category}, finalScore={selected_skill.final_score:.4f})"
        )

    lines.extend(
        [
            "",
            "## Routing Rule",
            "- 사용자 요청을 목표/도메인/제약으로 분해한 뒤, 가장 관련도 높은 primary skill 1개를 먼저 선택한다.",
            "- 복합 요청이면 secondary skill을 최대 2개까지 추가해 조합한다.",
            "",
            "## Response Rule",
            "- 응답 시작 시 사용한 skills 파일 경로를 먼저 명시한다.",
            "- skills 원문 기준으로 답변하고 임의 요약으로 대체하지 않는다.",
        ]
    )

    return "\\n".join(lines) + "\\n"


def _resolve_skill_relative_path(*, category: str, skill_id: int, used_paths: set[str]) -> str:
    base_name = _slug(category)
    candidate = f"skills/{base_name}.md"
    if candidate in used_paths:
        candidate = f"skills/{base_name}-{skill_id}.md"
    used_paths.add(candidate)
    return candidate


def _write_text(path: Path, content: str) -> WrittenFile:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    digest = hashlib.sha256(content.encode("utf-8")).hexdigest()
    return WrittenFile(path=path.as_posix(), length=len(content), sha256=digest)


def run_generation(*, spring_base_url: str, mcp_token: str, queries: list[str], output_dir: Path, timeout_seconds: float) -> dict[str, Any]:
    recommendation_response = _post_json(
        base_url=spring_base_url,
        path="/api/v1/mcp/recommendations",
        bearer_token=mcp_token,
        payload={"queries": queries},
        timeout_seconds=timeout_seconds,
    )
    selected_skills = _parse_selected_skills(recommendation_response)

    used_paths: set[str] = set()
    skill_paths: list[str] = []
    written_files: list[WrittenFile] = []

    for selected_skill in selected_skills:
        content_response = _post_json(
            base_url=spring_base_url,
            path="/api/v1/mcp/recommendations/skill-content",
            bearer_token=mcp_token,
            payload={"skillId": selected_skill.skill_id},
            timeout_seconds=timeout_seconds,
        )
        category, source_repo, raw_markdown = _extract_skill_content(content_response)

        relative_path = _resolve_skill_relative_path(
            category=category,
            skill_id=selected_skill.skill_id,
            used_paths=used_paths,
        )
        markdown = _build_skill_markdown(
            skill_id=selected_skill.skill_id,
            category=category,
            source_repo=source_repo,
            raw_content=raw_markdown,
        )
        written = _write_text(output_dir / relative_path, markdown)
        skill_paths.append(relative_path)
        written_files.append(written)

    agents_markdown = _build_agents_markdown(
        selected_skills=selected_skills,
        queries=queries,
        skill_paths=skill_paths,
    )
    written_agents = _write_text(output_dir / "agents.md", agents_markdown)
    written_files.append(written_agents)

    return {
        "success": True,
        "queries": queries,
        "selectedSkills": [
            {
                "skillId": skill.skill_id,
                "category": skill.category,
                "sourceRepo": skill.source_repo,
                "finalScore": skill.final_score,
            }
            for skill in selected_skills
        ],
        "writtenFiles": [
            {
                "path": wf.path,
                "length": wf.length,
                "sha256": wf.sha256,
            }
            for wf in written_files
        ],
    }


def _parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate skills/*.md and agents.md by calling MCP APIs directly.")
    parser.add_argument("--queries-json", required=True, help="JSON array string, e.g. '[\"SpringBoot\",\"infra\"]'")
    parser.add_argument(
        "--spring-base-url",
        default=os.getenv("SPRING_API_BASE_URL", "http://localhost:8080"),
        help="Spring API base URL",
    )
    parser.add_argument(
        "--mcp-token",
        default=os.getenv("MCP_PERSONAL_TOKEN"),
        help="MCP personal token (default: MCP_PERSONAL_TOKEN env)",
    )
    parser.add_argument("--output-dir", default=".", help="Output directory (default: current directory)")
    parser.add_argument("--timeout-seconds", type=float, default=20.0, help="HTTP timeout seconds")
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = _parse_args(argv)

    if not args.mcp_token or not str(args.mcp_token).strip():
        print("ERROR: mcp token is required. Use --mcp-token or MCP_PERSONAL_TOKEN env.", file=sys.stderr)
        return 2

    try:
        queries = _normalize_queries(args.queries_json)
        result = run_generation(
            spring_base_url=args.spring_base_url,
            mcp_token=str(args.mcp_token).strip(),
            queries=queries,
            output_dir=Path(args.output_dir),
            timeout_seconds=args.timeout_seconds,
        )
    except RunnerError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
