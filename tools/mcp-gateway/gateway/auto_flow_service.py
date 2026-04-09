from __future__ import annotations

import re
import uuid
from dataclasses import dataclass, field
from typing import Any

from gateway.input_normalizer import (
    GatewayValidationError,
    normalize_agent_type,
    normalize_flow_id,
    normalize_flow_step,
    normalize_queries,
    normalize_user_input_confirmed,
)
from gateway.spring_proxy_client import SpringProxyClient


@dataclass(frozen=True)
class SelectedSkill:
    category: str
    skill_id: int
    final_score: float
    source_repo: str


@dataclass
class AutoFlowState:
    flow_id: str
    agent_type: str
    current_step: str = "START"
    queries: list[str] = field(default_factory=list)
    selected_skills: list[SelectedSkill] = field(default_factory=list)


class AutoFlowService:
    DEFAULT_AGENT_TYPE = "CODEX"

    def __init__(self, spring_proxy_client: SpringProxyClient):
        self.spring_proxy_client = spring_proxy_client
        self._flows: dict[str, AutoFlowState] = {}

    def run(
        self,
        *,
        step: str,
        mcp_personal_token: str | None,
        agent_type: str | None,
        flow_id: str | None,
        queries: list[str] | tuple[str, ...] | None,
        user_input_confirmed: bool | None,
    ) -> dict[str, Any]:
        normalized_step = normalize_flow_step(step)

        if normalized_step == "START":
            return self._start(
                mcp_personal_token=mcp_personal_token,
                agent_type=agent_type,
            )

        if normalized_step == "COLLECTED":
            return self._collected(
                flow_id=flow_id,
                mcp_personal_token=mcp_personal_token,
                queries=queries,
                user_input_confirmed=user_input_confirmed,
            )

        raise GatewayValidationError(
            "Only START and COLLECTED steps are supported in direct-runner mode."
        )

    def _start(self, *, mcp_personal_token: str | None, agent_type: str | None) -> dict[str, Any]:
        normalized_agent_type = self._resolve_agent_type(agent_type)

        template_response = self.spring_proxy_client.get_start_agent_template(
            mcp_personal_token=mcp_personal_token,
            agent_type=normalized_agent_type,
        )
        template_name, version, template_markdown = self._extract_template(template_response)

        flow_id = f"flow_{uuid.uuid4().hex}"
        self._flows[flow_id] = AutoFlowState(
            flow_id=flow_id,
            agent_type=normalized_agent_type,
        )

        return {
            "success": True,
            "flowId": flow_id,
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
                "nextStepParamsExample": {
                    "step": "COLLECTED",
                    "flowId": flow_id,
                    "queries": ["SpringBoot", "infra", "DevOps"],
                    "userInputConfirmed": True,
                },
            },
        }

    def _collected(
        self,
        *,
        flow_id: str | None,
        mcp_personal_token: str | None,
        queries: list[str] | tuple[str, ...] | None,
        user_input_confirmed: bool | None,
    ) -> dict[str, Any]:
        flow = self._require_flow(flow_id)
        self._assert_step_allowed(flow, {"START"})
        normalize_user_input_confirmed(user_input_confirmed)
        normalized_queries = normalize_queries(queries)

        recommendation_response = self.spring_proxy_client.recommend_skills(
            mcp_personal_token=mcp_personal_token,
            queries=normalized_queries,
        )
        selected_skills = self._extract_selected_skills(recommendation_response)

        flow.queries = normalized_queries
        flow.selected_skills = selected_skills
        flow.current_step = "COLLECTED"

        expected_files = self._build_expected_files(selected_skills)

        return {
            "success": True,
            "flowId": flow.flow_id,
            "flowStep": "COLLECTED",
            "message": "추천 스킬 메타 조회 완료. 다음은 로컬 러너로 파일을 직접 생성하세요.",
            "recommendation": {
                "queries": normalized_queries,
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
            "runner": {
                "mode": "DIRECT_WRITE",
                "entrypoint": "tools/mcp-gateway/runner/generate_skills.py",
                "queries": normalized_queries,
                "expectedFiles": expected_files,
                "notes": [
                    "러너가 API를 직접 호출해 skills/*.md 와 agents.md를 생성합니다.",
                    "LLM이 skill 본문을 재작성하지 않습니다.",
                ],
            },
            "actions": {
                "writeFiles": [],
                "askUser": [
                    "로컬 러너를 실행해 skills/*.md와 agents.md를 자동 생성하세요.",
                    "예시: python3 tools/mcp-gateway/runner/generate_skills.py --queries-json='[\"SpringBoot\",\"infra\",\"DevOps\"]'",
                    "러너 실행 후 생성된 파일 목록만 사용자에게 보고하세요.",
                ],
                "nextStep": "DONE",
            },
        }

    def _require_flow(self, flow_id: str | None) -> AutoFlowState:
        normalized_flow_id = normalize_flow_id(flow_id)
        flow = self._flows.get(normalized_flow_id)
        if flow is None:
            raise GatewayValidationError("Unknown flowId. Start with start_auto_flow(step=START) first.")

        return flow

    def _assert_step_allowed(self, flow: AutoFlowState, allowed_steps: set[str]) -> None:
        if flow.current_step not in allowed_steps:
            allowed = ", ".join(sorted(allowed_steps))
            raise GatewayValidationError(
                f"Invalid step order. Current flow state is {flow.current_step}; allowed previous states: {allowed}."
            )

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
                )
            )

        return selected_skills

    def _build_expected_files(self, selected_skills: list[SelectedSkill]) -> list[str]:
        files: list[str] = []
        seen: set[str] = set()
        for selected_skill in selected_skills:
            base_name = self._slug(selected_skill.category)
            file_name = f"skills/{base_name}.md"
            if file_name in seen:
                file_name = f"skills/{base_name}-{selected_skill.skill_id}.md"
            files.append(file_name)
            seen.add(file_name)

        files.append("agents.md")
        return files

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

    def _slug(self, raw_value: str) -> str:
        normalized = re.sub(r"[^a-zA-Z0-9_-]+", "-", raw_value.strip().lower())
        normalized = normalized.strip("-")
        return normalized or "unknown"

    def _resolve_agent_type(self, request_agent_type: str | None) -> str:
        return normalize_agent_type(request_agent_type or self.DEFAULT_AGENT_TYPE)
