from __future__ import annotations

import hashlib
import re
import uuid
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from gateway.input_normalizer import (
    GatewayValidationError,
    normalize_agent_type,
    normalize_chunk_size,
    normalize_cursor,
    normalize_customization_applied,
    normalize_finalize_decision,
    normalize_flow_id,
    normalize_flow_step,
    normalize_queries,
    normalize_skill_id,
    normalize_user_decision_confirmed,
    normalize_user_input_confirmed,
    normalize_written_length,
    normalize_written_sha256,
)
from gateway.spring_proxy_client import SpringProxyClient


@dataclass(frozen=True)
class SelectedSkill:
    category: str
    skill_id: int
    final_score: float
    source_repo: str


@dataclass(frozen=True)
class SkillContent:
    category: str
    source_repo: str
    skill_md_raw: str


@dataclass
class AutoFlowState:
    flow_id: str
    agent_type: str
    current_step: str = "START"
    queries: list[str] = field(default_factory=list)
    selected_skills: list[SelectedSkill] = field(default_factory=list)
    skill_contents: dict[int, SkillContent] = field(default_factory=dict)
    fetched_skill_ids: set[int] = field(default_factory=set)
    verified_skill_ids: set[int] = field(default_factory=set)
    decision: str | None = None
    customization_notes: str = ""
    customization_applied: bool = False

    def selected_skill_ids(self) -> set[int]:
        return {skill.skill_id for skill in self.selected_skills}

    def has_all_skills_fetched(self) -> bool:
        selected_ids = self.selected_skill_ids()
        return bool(selected_ids) and selected_ids.issubset(self.fetched_skill_ids)

    def has_all_skills_verified(self) -> bool:
        selected_ids = self.selected_skill_ids()
        return bool(selected_ids) and selected_ids.issubset(self.verified_skill_ids)


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
        skill_id: int | float | str | None = None,
        cursor: int | float | str | None = None,
        chunk_size: int | float | str | None = None,
        written_length: int | float | str | None = None,
        written_sha256: str | None = None,
        user_decision_confirmed: bool | None = None,
        decision: str | None = None,
        customization_notes: str | None = None,
        customization_applied: bool | None = None,
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

        if normalized_step == "FETCH_SKILL":
            return self._fetch_skill(
                flow_id=flow_id,
                mcp_personal_token=mcp_personal_token,
                skill_id=skill_id,
                cursor=cursor,
                chunk_size=chunk_size,
            )

        if normalized_step == "VERIFY_SKILL":
            return self._verify_skill(
                flow_id=flow_id,
                skill_id=skill_id,
                written_length=written_length,
                written_sha256=written_sha256,
            )

        if normalized_step == "DECIDE":
            return self._decide(
                flow_id=flow_id,
                user_decision_confirmed=user_decision_confirmed,
                decision=decision,
                customization_notes=customization_notes,
            )

        return self._finalize(
            flow_id=flow_id,
            customization_applied=customization_applied,
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
        flow.skill_contents.clear()
        flow.fetched_skill_ids.clear()
        flow.verified_skill_ids.clear()
        flow.decision = None
        flow.customization_notes = ""
        flow.customization_applied = False
        flow.current_step = "COLLECTED"

        return {
            "success": True,
            "flowId": flow.flow_id,
            "flowStep": "COLLECTED",
            "message": "추천 스킬 메타 조회 완료 (본문은 FETCH_SKILL 단계에서 분할 전달)",
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
            "actions": {
                "writeFiles": [],
                "askUser": [
                    "selectedSkills의 skillId별로 FETCH_SKILL을 반복 호출해 파일을 완성하세요.",
                    "원문 보존을 위해 chunk를 임의 요약/축약하지 말고 write/append 그대로 반영하세요.",
                    "모든 skill 파일을 만든 뒤 VERIFY_SKILL 단계로 무결성 검증을 진행하세요.",
                ],
                "nextStep": "FETCH_SKILL",
                "nextStepParamsExample": {
                    "step": "FETCH_SKILL",
                    "flowId": flow.flow_id,
                    "skillId": selected_skills[0].skill_id,
                    "cursor": 0,
                    "chunkSize": 3000,
                },
            },
        }

    def _fetch_skill(
        self,
        *,
        flow_id: str | None,
        mcp_personal_token: str | None,
        skill_id: int | float | str | None,
        cursor: int | float | str | None,
        chunk_size: int | float | str | None,
    ) -> dict[str, Any]:
        flow = self._require_flow(flow_id)
        self._assert_step_allowed(flow, {"COLLECTED", "FETCH_SKILL", "VERIFY_SKILL"})
        normalized_skill_id = normalize_skill_id(skill_id)
        normalized_cursor = normalize_cursor(cursor)
        normalized_chunk_size = normalize_chunk_size(chunk_size)

        if normalized_skill_id not in flow.selected_skill_ids():
            raise GatewayValidationError("skillId is not included in current flow selectedSkills.")

        skill_content = self._get_skill_content(
            flow=flow,
            mcp_personal_token=mcp_personal_token,
            skill_id=normalized_skill_id,
        )
        safe_category = self._slug(skill_content.category or "unknown")
        path = f"skills/{safe_category}.md"
        full_markdown = self._build_skill_markdown_from_content(
            skill_id=normalized_skill_id,
            category=skill_content.category or "unknown",
            source_repo=skill_content.source_repo or "unknown",
            raw_content=skill_content.skill_md_raw,
        )

        chunk, next_cursor, has_next = self._slice_content(
            content=full_markdown,
            cursor=normalized_cursor,
            chunk_size=normalized_chunk_size,
        )
        if not chunk:
            raise GatewayValidationError("No remaining content for the given cursor.")

        if not has_next:
            flow.fetched_skill_ids.add(normalized_skill_id)
        flow.current_step = "FETCH_SKILL"

        expected_hash = hashlib.sha256(full_markdown.encode("utf-8")).hexdigest()
        has_all_fetched = flow.has_all_skills_fetched()
        next_unfetched_id = self._find_next_skill_id(
            flow=flow,
            excluded=flow.fetched_skill_ids,
        )

        next_step = "FETCH_SKILL"
        next_step_params_example: dict[str, Any]
        ask_user: list[str]

        if has_next:
            next_step_params_example = {
                "step": "FETCH_SKILL",
                "flowId": flow.flow_id,
                "skillId": normalized_skill_id,
                "cursor": next_cursor,
                "chunkSize": normalized_chunk_size,
            }
            ask_user = [
                "같은 skillId로 hasNext=false가 될 때까지 FETCH_SKILL을 반복하세요.",
            ]
        elif not has_all_fetched and next_unfetched_id is not None:
            next_step_params_example = {
                "step": "FETCH_SKILL",
                "flowId": flow.flow_id,
                "skillId": next_unfetched_id,
                "cursor": 0,
                "chunkSize": normalized_chunk_size,
            }
            ask_user = [
                "현재 skill 파일은 완료되었습니다. 다음 skillId로 FETCH_SKILL을 진행하세요.",
            ]
        else:
            next_step = "VERIFY_SKILL"
            first_verifiable_id = self._find_next_skill_id(
                flow=flow,
                excluded=flow.verified_skill_ids,
            ) or normalized_skill_id
            next_step_params_example = {
                "step": "VERIFY_SKILL",
                "flowId": flow.flow_id,
                "skillId": first_verifiable_id,
                "writtenLength": len(full_markdown),
                "writtenSha256": expected_hash,
            }
            ask_user = [
                "모든 skills 파일 생성이 완료되었습니다. 각 파일의 길이/sha256으로 VERIFY_SKILL을 진행하세요.",
            ]

        return {
            "success": True,
            "flowId": flow.flow_id,
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
                "totalLength": len(full_markdown),
            },
            "integrity": {
                "expectedFileLength": len(full_markdown),
                "expectedFileSha256": expected_hash,
                "hashAlgorithm": "sha256",
            },
            "actions": {
                "writeFiles": [
                    {
                        "path": path,
                        "content": chunk,
                        "mode": "write" if normalized_cursor == 0 else "append",
                        "reason": "추천 스킬 원문 chunk 저장",
                    }
                ],
                "askUser": ask_user,
                "nextStep": next_step,
                "nextStepParamsExample": next_step_params_example,
            },
        }

    def _verify_skill(
        self,
        *,
        flow_id: str | None,
        skill_id: int | float | str | None,
        written_length: int | float | str | None,
        written_sha256: str | None,
    ) -> dict[str, Any]:
        flow = self._require_flow(flow_id)
        self._assert_step_allowed(flow, {"FETCH_SKILL", "VERIFY_SKILL"})
        normalized_skill_id = normalize_skill_id(skill_id)
        normalized_written_length = normalize_written_length(written_length)
        normalized_written_sha256 = normalize_written_sha256(written_sha256)

        if normalized_skill_id not in flow.selected_skill_ids():
            raise GatewayValidationError("skillId is not included in current flow selectedSkills.")

        if normalized_skill_id not in flow.fetched_skill_ids:
            raise GatewayValidationError("skillId file is not completed yet. Finish FETCH_SKILL first.")

        skill_content = self._get_skill_content(
            flow=flow,
            mcp_personal_token=None,
            skill_id=normalized_skill_id,
        )
        expected_content = self._build_skill_markdown_from_content(
            skill_id=normalized_skill_id,
            category=skill_content.category or "unknown",
            source_repo=skill_content.source_repo or "unknown",
            raw_content=skill_content.skill_md_raw,
        )
        expected_length = len(expected_content)
        expected_hash = hashlib.sha256(expected_content.encode("utf-8")).hexdigest()

        if normalized_written_length != expected_length or normalized_written_sha256 != expected_hash:
            raise GatewayValidationError(
                "Skill file integrity mismatch. Do not summarize/truncate raw content; write exact chunk output."
            )

        flow.verified_skill_ids.add(normalized_skill_id)
        flow.current_step = "VERIFY_SKILL"

        all_verified = flow.has_all_skills_verified()
        next_unverified_id = self._find_next_skill_id(
            flow=flow,
            excluded=flow.verified_skill_ids,
        )

        next_step = "DECIDE" if all_verified else "VERIFY_SKILL"
        next_step_params_example: dict[str, Any]
        ask_user: list[str]

        if all_verified:
            next_step_params_example = {
                "step": "DECIDE",
                "flowId": flow.flow_id,
                "userDecisionConfirmed": True,
                "decision": "ACCEPT",
                "customizationNotes": "",
            }
            ask_user = [
                "사용자에게 '이대로 진행(ACCEPT)' 또는 '사용자 맞춤 보정(CUSTOMIZE)' 결정을 받아 DECIDE를 호출하세요.",
            ]
        else:
            next_step_params_example = {
                "step": "VERIFY_SKILL",
                "flowId": flow.flow_id,
                "skillId": next_unverified_id,
                "writtenLength": "해당 파일 길이",
                "writtenSha256": "해당 파일 sha256",
            }
            ask_user = [
                "다음 skill 파일 무결성을 VERIFY_SKILL로 검증하세요.",
            ]

        return {
            "success": True,
            "flowId": flow.flow_id,
            "flowStep": "VERIFY_SKILL",
            "message": "skill 파일 무결성 검증 완료",
            "verification": {
                "skillId": normalized_skill_id,
                "verified": True,
                "verifiedCount": len(flow.verified_skill_ids),
                "totalCount": len(flow.selected_skills),
            },
            "actions": {
                "writeFiles": [],
                "askUser": ask_user,
                "nextStep": next_step,
                "nextStepParamsExample": next_step_params_example,
            },
        }

    def _decide(
        self,
        *,
        flow_id: str | None,
        user_decision_confirmed: bool | None,
        decision: str | None,
        customization_notes: str | None,
    ) -> dict[str, Any]:
        flow = self._require_flow(flow_id)
        self._assert_step_allowed(flow, {"VERIFY_SKILL", "DECIDE"})
        if not flow.has_all_skills_verified():
            raise GatewayValidationError("All selected skill files must be verified before DECIDE step.")

        normalize_user_decision_confirmed(user_decision_confirmed)
        normalized_decision = normalize_finalize_decision(decision)
        normalized_notes = (customization_notes or "").strip()
        if normalized_decision == "CUSTOMIZE" and not normalized_notes:
            raise GatewayValidationError("customizationNotes is required when decision is CUSTOMIZE.")

        flow.decision = normalized_decision
        flow.customization_notes = normalized_notes
        flow.customization_applied = False
        flow.current_step = "DECIDE"

        ask_user = [
            "FINALIZE 호출 전, 사용자 결정에 맞는 최종 파일 반영을 완료하세요.",
        ]
        if normalized_decision == "CUSTOMIZE":
            ask_user = [
                "기존 skills 파일을 기반으로 필요한 섹션만 부분 보정하세요.",
                "보정 완료 후 FINALIZE에서 customizationApplied=true로 호출하세요.",
            ]

        return {
            "success": True,
            "flowId": flow.flow_id,
            "flowStep": "DECIDE",
            "message": "사용자 결정이 저장되었습니다. FINALIZE 단계로 진행하세요.",
            "decision": {
                "decision": normalized_decision,
                "customizationNotes": normalized_notes,
            },
            "actions": {
                "writeFiles": [],
                "askUser": ask_user,
                "nextStep": "FINALIZE",
                "nextStepParamsExample": {
                    "step": "FINALIZE",
                    "flowId": flow.flow_id,
                    "customizationApplied": normalized_decision != "CUSTOMIZE",
                },
            },
        }

    def _finalize(self, *, flow_id: str | None, customization_applied: bool | None) -> dict[str, Any]:
        flow = self._require_flow(flow_id)
        self._assert_step_allowed(flow, {"DECIDE", "FINALIZE"})
        if flow.decision is None:
            raise GatewayValidationError("DECIDE step must be completed before FINALIZE.")

        normalized_customization_applied = normalize_customization_applied(customization_applied)
        if flow.decision == "CUSTOMIZE" and not normalized_customization_applied:
            raise GatewayValidationError(
                "customizationApplied=true is required for FINALIZE when decision is CUSTOMIZE."
            )

        flow.customization_applied = normalized_customization_applied
        flow.current_step = "FINALIZE"

        agents_markdown = self._build_agents_markdown(
            selected_skills=flow.selected_skills,
            queries=flow.queries,
            decision=flow.decision,
            customization_notes=flow.customization_notes,
        )

        return {
            "success": True,
            "flowId": flow.flow_id,
            "flowStep": "FINALIZE",
            "message": "최종 agents.md 생성 준비 완료",
            "finalize": {
                "decision": flow.decision,
                "queries": flow.queries,
                "customizationNotes": flow.customization_notes,
                "customizationApplied": flow.customization_applied,
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

    def _get_skill_content(
        self,
        *,
        flow: AutoFlowState,
        mcp_personal_token: str | None,
        skill_id: int,
    ) -> SkillContent:
        cached = flow.skill_contents.get(skill_id)
        if cached is not None:
            return cached

        response = self.spring_proxy_client.get_recommendation_skill_content(
            mcp_personal_token=mcp_personal_token,
            skill_id=skill_id,
        )
        parsed = self._extract_skill_content(response)
        flow.skill_contents[skill_id] = parsed
        return parsed

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

    def _build_agents_markdown(
        self,
        *,
        selected_skills: list[SelectedSkill],
        queries: list[str],
        decision: str,
        customization_notes: str,
    ) -> str:
        lines = [
            "# AGENTS Routing",
            "",
            "## Generation Context",
            f"- decision: {decision}",
            f"- queries: {', '.join(queries)}",
            f"- customizationNotes: {customization_notes}",
            "",
            "## Skill Inventory",
        ]

        for selected_skill in selected_skills:
            safe_category = self._slug(selected_skill.category)
            skill_path = f"skills/{safe_category}.md"
            lines.append(
                f"- {skill_path} (category={selected_skill.category}, finalScore={selected_skill.final_score:.4f})"
            )

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

    def _find_next_skill_id(self, *, flow: AutoFlowState, excluded: set[int]) -> int | None:
        for selected_skill in flow.selected_skills:
            if selected_skill.skill_id not in excluded:
                return selected_skill.skill_id

        return None

    def _slug(self, raw_value: str) -> str:
        normalized = re.sub(r"[^a-zA-Z0-9_-]+", "-", raw_value.strip().lower())
        normalized = normalized.strip("-")
        return normalized or "unknown"

    def _resolve_agent_type(self, request_agent_type: str | None) -> str:
        return normalize_agent_type(request_agent_type or self.DEFAULT_AGENT_TYPE)

    def _discover_skill_paths(self) -> list[str]:
        skills_dir = Path("skills")
        if not skills_dir.exists() or not skills_dir.is_dir():
            return []

        return sorted(path.as_posix() for path in skills_dir.glob("*.md"))

    def _infer_category_from_path(self, skill_path: str) -> str:
        file_name = skill_path.rsplit("/", 1)[-1]
        category = file_name.removesuffix(".md").strip()
        return category or "unknown"

    def _build_skill_markdown_from_content(
        self,
        *,
        skill_id: int,
        category: str,
        source_repo: str,
        raw_content: str,
    ) -> str:
        return (
            f"# Skill: {category} ({skill_id})\n\n"
            f"- category: {category}\n"
            f"- skillId: {skill_id}\n"
            f"- sourceRepo: {source_repo}\n\n"
            "## Skill Markdown\n"
            f"{raw_content}"
        )
