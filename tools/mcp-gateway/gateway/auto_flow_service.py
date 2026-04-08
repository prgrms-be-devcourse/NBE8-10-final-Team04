from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from gateway.input_normalizer import (
    GatewayValidationError,
    normalize_agent_type,
    normalize_chunk_size,
    normalize_cursor,
    normalize_finalize_decision,
    normalize_flow_step,
    normalize_keywords,
    normalize_skill_id,
    normalize_user_input_confirmed,
)
from gateway.spring_proxy_client import SpringProxyClient


@dataclass(frozen=True)
class SelectedSkill:
    category: str
    skill_id: int
    final_score: float
    source_repo: str
    skill_md_raw: str


@dataclass(frozen=True)
class SkillContent:
    category: str
    source_repo: str
    skill_md_raw: str


class AutoFlowService:
    DEFAULT_AGENT_TYPE = "CODEX"

    def __init__(self, spring_proxy_client: SpringProxyClient):
        self.spring_proxy_client = spring_proxy_client

    def run(
        self,
        *,
        step: str,
        mcp_personal_token: str | None,
        agent_type: str | None,
        keywords: str | None,
        user_input_confirmed: bool | None,
        skill_id: int | None = None,
        cursor: int | None = None,
        chunk_size: int | None = None,
        decision: str | None = None,
        customization_notes: str | None = None,
    ) -> dict[str, Any]:
        normalized_step = normalize_flow_step(step)

        if normalized_step == "START":
            return self._start(
                mcp_personal_token=mcp_personal_token,
                agent_type=agent_type,
            )

        if normalized_step == "COLLECTED":
            return self._collected(
                mcp_personal_token=mcp_personal_token,
                keywords=keywords,
                user_input_confirmed=user_input_confirmed,
            )

        if normalized_step == "FETCH_SKILL":
            return self._fetch_skill(
                mcp_personal_token=mcp_personal_token,
                skill_id=skill_id,
                cursor=cursor,
                chunk_size=chunk_size,
            )

        return self._finalize(
            mcp_personal_token=mcp_personal_token,
            keywords=keywords,
            decision=decision,
            customization_notes=customization_notes,
        )

    def _start(self, *, mcp_personal_token: str | None, agent_type: str | None) -> dict[str, Any]:
        normalized_agent_type = self._resolve_agent_type(agent_type)

        template_response = self.spring_proxy_client.get_start_agent_template(
            mcp_personal_token=mcp_personal_token,
            agent_type=normalized_agent_type,
        )
        template_name, version, template_markdown = self._extract_template(template_response)

        return {
            "success": True,
            "flowStep": "START",
            "message": "start-agent 템플릿 조회 및 파일 생성 준비 완료",
            "templateMeta": {
                "templateName": template_name,
                "version": version,
            },
            "actions": {
                "writeFiles": [
                    {
                        "path": "start.agent.md",
                        "content": template_markdown,
                        "reason": "자동 플로우 시작 템플릿 생성",
                    }
                ],
                "askUser": [
                    "만들고 싶은 프로젝트 목표가 무엇인가요?",
                    "사용할 기술 스택/언어는 무엇인가요?",
                    "필수 제약(예: OCI, Docker, 기간, 비용)이 있나요?",
                    "기획 입력이 끝났으면 '기획 입력 끝'이라고 알려주세요.",
                ],
                "nextStep": "COLLECTED",
            },
        }

    def _collected(
        self,
        *,
        mcp_personal_token: str | None,
        keywords: str | None,
        user_input_confirmed: bool | None,
    ) -> dict[str, Any]:
        normalize_user_input_confirmed(user_input_confirmed)
        normalized_keywords = normalize_keywords(keywords)

        recommendation_response = self.spring_proxy_client.recommend_skills(
            mcp_personal_token=mcp_personal_token,
            keywords=normalized_keywords,
        )

        selected_skills = self._extract_selected_skills(recommendation_response)

        return {
            "success": True,
            "flowStep": "COLLECTED",
            "message": "추천 스킬 메타 조회 완료 (본문은 FETCH_SKILL 단계에서 분할 전달)",
            "recommendation": {
                "keywords": normalized_keywords,
                "selectedSkills": [
                    {
                        "category": skill.category,
                        "skillId": skill.skill_id,
                        "finalScore": skill.final_score,
                        "sourceRepo": skill.source_repo,
                    }
                    for skill in selected_skills
                ],
            },
            "actions": {
                "writeFiles": [],
                "askUser": [
                    "각 selectedSkills의 skillId에 대해 FETCH_SKILL(step=FETCH_SKILL)을 호출해 skills 파일을 생성하세요.",
                    "FETCH_SKILL은 chunk 단위로 본문을 전달합니다. hasNext=true인 동안 같은 skillId로 반복 호출하세요.",
                    "모든 skills 파일 생성이 끝난 뒤 사용자에게 진행/보정 여부를 물은 다음 FINALIZE를 호출하세요.",
                ],
                "nextStep": "FETCH_SKILL",
                "nextStepParamsExample": {
                    "step": "FETCH_SKILL",
                    "skillId": selected_skills[0].skill_id,
                    "cursor": 0,
                    "chunkSize": 3000,
                },
            },
        }

    def _finalize(
        self,
        *,
        mcp_personal_token: str | None,
        keywords: str | None,
        decision: str | None,
        customization_notes: str | None,
    ) -> dict[str, Any]:
        normalized_keywords = normalize_keywords(keywords)
        normalized_decision = normalize_finalize_decision(decision)

        agents_markdown = self._build_agents_markdown(
            selected_skills=None,
            keywords=normalized_keywords,
            decision=normalized_decision,
            customization_notes=customization_notes,
        )

        return {
            "success": True,
            "flowStep": "FINALIZE",
            "message": "최종 agents.md 생성 준비 완료",
            "finalize": {
                "decision": normalized_decision,
                "keywords": normalized_keywords,
                "customizationNotes": customization_notes or "",
                "customizationPolicy": (
                    "CUSTOMIZE 선택 시 기존 skills 파일을 기반으로 필요한 부분만 보정하고, "
                    "새 문서를 처음부터 작성하지 않습니다."
                ),
            },
            "actions": {
                "writeFiles": [
                    {
                        "path": "agents.md",
                        "content": agents_markdown,
                        "reason": "최종 라우터 파일 덮어쓰기",
                    },
                ],
                "deleteFiles": ["start.agent.md"],
            },
        }

    def _fetch_skill(
        self,
        *,
        mcp_personal_token: str | None,
        skill_id: int | None,
        cursor: int | None,
        chunk_size: int | None,
    ) -> dict[str, Any]:
        normalized_skill_id = normalize_skill_id(skill_id)
        normalized_cursor = normalize_cursor(cursor)
        normalized_chunk_size = normalize_chunk_size(chunk_size)

        content_response = self.spring_proxy_client.get_recommendation_skill_content(
            mcp_personal_token=mcp_personal_token,
            skill_id=normalized_skill_id,
        )
        skill_content = self._extract_skill_content(content_response)
        safe_category = self._slug(skill_content.category or "unknown")
        path = f"skills/{safe_category}.md"

        chunk, next_cursor, has_next = self._slice_content(
            content=skill_content.skill_md_raw,
            cursor=normalized_cursor,
            chunk_size=normalized_chunk_size,
        )
        if not chunk:
            raise GatewayValidationError("No remaining content for the given cursor.")

        write_mode = "write" if normalized_cursor == 0 else "append"
        write_content = self._build_first_chunk_prefix(
            skill_id=normalized_skill_id,
            category=skill_content.category or "unknown",
            source_repo=skill_content.source_repo or "unknown",
            first_chunk=chunk,
        ) if write_mode == "write" else chunk

        ask_user = [
            "같은 skillId로 hasNext=false가 될 때까지 FETCH_SKILL을 반복하세요.",
        ]
        if not has_next:
            ask_user = [
                "이 skill 파일 생성을 완료했습니다. 다음 selectedSkills의 skillId로 FETCH_SKILL을 진행하세요.",
                "모든 skills 파일 생성 완료 후 사용자에게 진행/보정 여부를 확인하고 FINALIZE를 호출하세요.",
            ]

        return {
            "success": True,
            "flowStep": "FETCH_SKILL",
            "message": "스킬 본문 chunk 전달 완료",
            "skillChunk": {
                "skillId": normalized_skill_id,
                "category": skill_content.category,
                "sourceRepo": skill_content.source_repo,
                "path": path,
                "cursor": normalized_cursor,
                "nextCursor": next_cursor,
                "chunkSize": normalized_chunk_size,
                "hasNext": has_next,
                "totalLength": len(skill_content.skill_md_raw),
            },
            "actions": {
                "writeFiles": [
                    {
                        "path": path,
                        "content": write_content,
                        "mode": write_mode,
                        "reason": "추천 스킬 원문 chunk 저장",
                    }
                ],
                "askUser": ask_user,
                "nextStep": "FETCH_SKILL" if has_next else "FINALIZE",
                "nextStepParamsExample": (
                    {
                        "step": "FETCH_SKILL",
                        "skillId": normalized_skill_id,
                        "cursor": next_cursor,
                        "chunkSize": normalized_chunk_size,
                    } if has_next else {
                        "step": "FINALIZE",
                        "keywords": "기존 COLLECTED keywords 값 사용",
                        "decision": "ACCEPT",
                        "customizationNotes": "optional",
                    }
                ),
            },
        }

    def _extract_template(self, response: dict[str, Any]) -> tuple[str, str, str]:
        payload = response.get("data", response)
        if not isinstance(payload, dict):
            raise GatewayValidationError("start-agent response data must be a JSON object.")

        template_name = payload.get("templateName", "start.agent.md")
        version = payload.get("version", "v1")
        template_markdown = payload.get("templateMarkdown")

        if not isinstance(template_markdown, str) or not template_markdown.strip():
            raise GatewayValidationError("templateMarkdown is missing in start-agent response.")

        return str(template_name), str(version), template_markdown

    def _extract_selected_skills(self, response: dict[str, Any]) -> list[SelectedSkill]:
        payload = response.get("data", response)
        if not isinstance(payload, dict):
            raise GatewayValidationError("recommendation response data must be a JSON object.")

        raw_selected_skills = payload.get("selectedSkills")
        if not isinstance(raw_selected_skills, list) or not raw_selected_skills:
            raise GatewayValidationError("selectedSkills is missing or empty in recommendation response.")

        selected_skills: list[SelectedSkill] = []
        for raw_skill in raw_selected_skills:
            if not isinstance(raw_skill, dict):
                raise GatewayValidationError("selectedSkills item must be a JSON object.")

            category = str(raw_skill.get("category", "unknown")).strip() or "unknown"
            skill_id = self._parse_skill_id(raw_skill.get("skillId"))
            if skill_id <= 0:
                raise GatewayValidationError("skillId is missing or invalid in selectedSkills item.")
            source_repo = str(raw_skill.get("sourceRepo", "unknown")).strip() or "unknown"

            try:
                final_score = float(raw_skill.get("finalScore", 0.0))
            except (TypeError, ValueError):
                final_score = 0.0

            selected_skills.append(
                SelectedSkill(
                    category=category,
                    skill_id=skill_id,
                    final_score=final_score,
                    source_repo=source_repo,
                    skill_md_raw="",
                )
            )

        return selected_skills

    def _resolve_selected_skill_contents(
        self,
        *,
        mcp_personal_token: str | None,
        selected_skills_summary: list[SelectedSkill],
    ) -> list[SelectedSkill]:
        resolved_skills: list[SelectedSkill] = []

        for skill_summary in selected_skills_summary:
            content_response = self.spring_proxy_client.get_recommendation_skill_content(
                mcp_personal_token=mcp_personal_token,
                skill_id=skill_summary.skill_id,
            )
            skill_content = self._extract_skill_content(content_response)
            resolved_skills.append(
                SelectedSkill(
                    category=skill_content.category or skill_summary.category,
                    skill_id=skill_summary.skill_id,
                    final_score=skill_summary.final_score,
                    source_repo=skill_content.source_repo or skill_summary.source_repo,
                    skill_md_raw=skill_content.skill_md_raw,
                )
            )

        return resolved_skills

    def _extract_skill_content(self, response: dict[str, Any]) -> SkillContent:
        payload = response.get("data", response)
        if not isinstance(payload, dict):
            raise GatewayValidationError("skill content response data must be a JSON object.")

        category = str(payload.get("category", "")).strip()
        source_repo = str(payload.get("sourceRepo", "")).strip()
        skill_md_raw = str(payload.get("skillMdRaw", "")).strip()
        if not skill_md_raw:
            raise GatewayValidationError("skillMdRaw is missing in skill content response.")

        return SkillContent(
            category=category,
            source_repo=source_repo,
            skill_md_raw=skill_md_raw,
        )

    def _slice_content(self, *, content: str, cursor: int, chunk_size: int) -> tuple[str, int, bool]:
        content_length = len(content)
        if cursor >= content_length:
            return "", cursor, False

        chunk_end = min(cursor + chunk_size, content_length)
        chunk = content[cursor:chunk_end]
        has_next = chunk_end < content_length
        return chunk, chunk_end, has_next

    def _build_first_chunk_prefix(self, *, skill_id: int, category: str, source_repo: str, first_chunk: str) -> str:
        return (
            f"# Skill: {category} ({skill_id})\n\n"
            f"- category: {category}\n"
            f"- skillId: {skill_id}\n"
            f"- sourceRepo: {source_repo}\n\n"
            "## Skill Markdown\n"
            f"{first_chunk}"
        )

    def _parse_skill_id(self, raw_skill_id: Any) -> int:
        if isinstance(raw_skill_id, bool):
            return 0

        if isinstance(raw_skill_id, int):
            return max(raw_skill_id, 0)

        if isinstance(raw_skill_id, float):
            if raw_skill_id.is_integer():
                return max(int(raw_skill_id), 0)
            return 0

        if isinstance(raw_skill_id, str):
            normalized = raw_skill_id.strip()
            if normalized.lstrip("-").isdigit():
                return max(int(normalized), 0)

        return 0

    def _build_skill_file_action(self, selected_skill: SelectedSkill) -> dict[str, str]:
        safe_category = self._slug(selected_skill.category)
        path = f"skills/{safe_category}.md"
        content = self._build_skill_markdown(selected_skill)

        return {
            "path": path,
            "content": content,
            "reason": "추천 스킬 원문 파일 생성",
        }

    def _build_skill_markdown(self, selected_skill: SelectedSkill) -> str:
        return (
            f"# Skill: {selected_skill.category} ({selected_skill.skill_id})\n\n"
            f"- category: {selected_skill.category}\n"
            f"- skillId: {selected_skill.skill_id}\n"
            f"- finalScore: {selected_skill.final_score:.4f}\n"
            f"- sourceRepo: {selected_skill.source_repo}\n\n"
            "## Skill Markdown\n"
            f"{selected_skill.skill_md_raw}\n"
        )

    def _build_agents_markdown(
        self,
        *,
        selected_skills: list[SelectedSkill] | None,
        keywords: str,
        decision: str,
        customization_notes: str | None,
    ) -> str:
        lines = [
            "# AGENTS Routing",
            "",
            "## Generation Context",
            f"- decision: {decision}",
            f"- keywords: {keywords}",
            f"- customizationNotes: {customization_notes or ''}",
            "",
            "## Skill Inventory",
        ]

        inventory_paths: list[str] = []
        if selected_skills:
            for selected_skill in selected_skills:
                safe_category = self._slug(selected_skill.category)
                skill_path = f"skills/{safe_category}.md"
                inventory_paths.append(skill_path)
                lines.append(f"- {skill_path} (category={selected_skill.category}, finalScore={selected_skill.final_score:.4f})")
        else:
            inventory_paths = self._discover_skill_paths()
            if inventory_paths:
                for skill_path in inventory_paths:
                    inferred_category = self._infer_category_from_path(skill_path)
                    lines.append(f"- {skill_path} (category={inferred_category})")
            else:
                lines.append("- skills/*.md (COLLECTED 단계에서 이미 생성된 파일 기준)")

        lines.extend(
            [
                "",
                "## Routing Rule",
                "- 사용자 요청을 목표/도메인/제약으로 분해한 뒤, 가장 관련도 높은 `primary` skill 1개를 먼저 선택한다.",
                "- 복합 요청이면 `secondary` skill을 최대 2개까지 추가해 조합한다.",
                "- 선택 기준 우선순위: 사용자 제약 > 보안/안정성 > 기능 정확성 > 유지보수성 > 구현 속도.",
                "- 충돌 시 상위 우선순위를 유지하고, 하위 우선순위 항목은 타협 또는 제외한다.",
                "- 선택 근거가 약하면 임의 생성하지 말고, inventory 내 가장 근접한 skill을 선택해 한계를 함께 보고한다.",
                "- inventory 외 문서를 새로 만들거나 외부 정보를 임의로 섞지 않는다(사용자 명시 요청 제외).",
                "",
                "## Response Rule",
                "- 응답 시작 시 사용한 `primary/secondary` skills 파일 경로를 먼저 명시한다.",
                "- 각 스킬에서 어떤 섹션/규칙을 적용했는지 요약한 뒤 최종 답변을 제공한다.",
                "- 코드/명령/경로는 skills 원문 기준을 우선 적용하고, 변경 시 근거를 짧게 남긴다.",
            ]
        )

        if decision == "CUSTOMIZE":
            lines.extend(
                [
                    "",
                    "## Customize Rule",
                    "- 기존 `skills/*.md`를 기준으로 필요한 섹션만 부분 보정한다.",
                    "- 기존 문서 구조, 코드 블록, 파일 경로, 클래스/함수명은 최대한 유지한다.",
                    "- 새 문서를 처음부터 다시 작성하지 않는다.",
                ]
            )

        return "\n".join(lines) + "\n"

    def _discover_skill_paths(self) -> list[str]:
        skills_dir = Path("skills")
        if not skills_dir.exists() or not skills_dir.is_dir():
            return []

        return sorted(path.as_posix() for path in skills_dir.glob("*.md"))

    def _infer_category_from_path(self, skill_path: str) -> str:
        file_name = skill_path.rsplit("/", 1)[-1]
        category = file_name.removesuffix(".md").strip()
        return category or "unknown"

    def _slug(self, raw_value: str) -> str:
        normalized = re.sub(r"[^a-zA-Z0-9_-]+", "-", raw_value.strip().lower())
        normalized = normalized.strip("-")
        return normalized or "unknown"

    def _resolve_agent_type(self, request_agent_type: str | None) -> str:
        return normalize_agent_type(request_agent_type or self.DEFAULT_AGENT_TYPE)
