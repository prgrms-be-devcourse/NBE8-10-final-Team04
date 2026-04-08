from __future__ import annotations

SUPPORTED_AGENT_TYPES = {"CLAUDE", "CODEX", "GEMINI"}
SUPPORTED_FLOW_STEPS = {"START", "COLLECTED", "FETCH_SKILL", "FINALIZE"}
SUPPORTED_FINALIZE_DECISIONS = {"ACCEPT", "CUSTOMIZE"}


class GatewayValidationError(ValueError):
    """Raised when tool input is invalid."""


def normalize_mcp_personal_token(token: str) -> str:
    if token is None:
        raise GatewayValidationError("mcpPersonalToken is required.")

    normalized = token.strip()
    if not normalized:
        raise GatewayValidationError("mcpPersonalToken must not be blank.")

    return normalized


def normalize_keywords(keywords: str) -> str:
    if keywords is None:
        raise GatewayValidationError("keywords is required.")

    normalized = " ".join(keywords.split())
    if not normalized:
        raise GatewayValidationError("keywords must not be blank.")

    return normalized


def normalize_agent_type(agent_type: str) -> str:
    if agent_type is None:
        raise GatewayValidationError("agentType is required.")

    normalized = agent_type.strip().upper()
    if normalized not in SUPPORTED_AGENT_TYPES:
        raise GatewayValidationError(
            "agentType must be one of: CLAUDE, CODEX, GEMINI."
        )

    return normalized


def normalize_flow_step(step: str) -> str:
    if step is None:
        raise GatewayValidationError("step is required.")

    normalized = step.strip().upper()
    if normalized not in SUPPORTED_FLOW_STEPS:
        raise GatewayValidationError(
            "step must be one of: START, COLLECTED, FINALIZE."
        )

    return normalized


def normalize_finalize_decision(decision: str) -> str:
    if decision is None:
        raise GatewayValidationError("decision is required for FINALIZE step.")

    normalized = decision.strip().upper()
    if normalized not in SUPPORTED_FINALIZE_DECISIONS:
        raise GatewayValidationError(
            "decision must be one of: ACCEPT, CUSTOMIZE."
        )

    return normalized


def normalize_user_input_confirmed(user_input_confirmed: bool | None) -> bool:
    if user_input_confirmed is not True:
        raise GatewayValidationError(
            "userInputConfirmed must be true for COLLECTED step."
        )

    return True


def normalize_skill_id(skill_id: int | None) -> int:
    if skill_id is None or skill_id <= 0:
        raise GatewayValidationError("skillId must be a positive integer for FETCH_SKILL step.")

    return skill_id


def normalize_cursor(cursor: int | None) -> int:
    if cursor is None:
        return 0

    if cursor < 0:
        raise GatewayValidationError("cursor must be greater than or equal to 0.")

    return cursor


def normalize_chunk_size(chunk_size: int | None) -> int:
    if chunk_size is None:
        return 3000

    if chunk_size <= 0:
        raise GatewayValidationError("chunkSize must be a positive integer.")

    if chunk_size > 20000:
        raise GatewayValidationError("chunkSize must be less than or equal to 20000.")

    return chunk_size
