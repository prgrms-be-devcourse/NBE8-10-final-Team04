from __future__ import annotations

import re

SUPPORTED_AGENT_TYPES = {"CLAUDE", "CODEX", "GEMINI"}
SUPPORTED_FLOW_STEPS = ("START", "COLLECTED", "FETCH_SKILL", "VERIFY_SKILL", "DECIDE", "FINALIZE")
SUPPORTED_FINALIZE_DECISIONS = {"ACCEPT", "CUSTOMIZE"}
SHA256_PATTERN = re.compile(r"^[0-9a-fA-F]{64}$")


class GatewayValidationError(ValueError):
    """Raised when tool input is invalid."""


def normalize_mcp_personal_token(token: str) -> str:
    if token is None:
        raise GatewayValidationError("mcpPersonalToken is required.")

    normalized = token.strip()
    if not normalized:
        raise GatewayValidationError("mcpPersonalToken must not be blank.")

    return normalized


def normalize_queries(queries: list[str] | tuple[str, ...] | None) -> list[str]:
    if queries is None:
        raise GatewayValidationError("queries is required.")

    if not isinstance(queries, (list, tuple)):
        raise GatewayValidationError("queries must be an array of strings.")

    normalized_queries: list[str] = []
    for query in queries:
        if not isinstance(query, str):
            raise GatewayValidationError("queries must contain only strings.")

        normalized = " ".join(query.split())
        if not normalized:
            raise GatewayValidationError("queries must not contain blank values.")

        if len(normalized) > 100:
            raise GatewayValidationError("each query must be 100 characters or fewer.")

        normalized_queries.append(normalized)

    if not normalized_queries:
        raise GatewayValidationError("queries must contain at least one item.")

    if len(normalized_queries) > 7:
        raise GatewayValidationError("queries must contain 7 items or fewer.")

    return normalized_queries


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
    if normalized not in set(SUPPORTED_FLOW_STEPS):
        raise GatewayValidationError(
            f"step must be one of: {', '.join(SUPPORTED_FLOW_STEPS)}."
        )

    return normalized


def normalize_flow_id(flow_id: str | None) -> str:
    if flow_id is None:
        raise GatewayValidationError("flowId is required for this step.")

    normalized = flow_id.strip()
    if not normalized:
        raise GatewayValidationError("flowId must not be blank.")

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


def normalize_user_decision_confirmed(user_decision_confirmed: bool | None) -> bool:
    if user_decision_confirmed is not True:
        raise GatewayValidationError(
            "userDecisionConfirmed must be true for DECIDE step."
        )

    return True


def normalize_skill_id(skill_id: int | float | str | None) -> int:
    normalized = _normalize_int(skill_id)
    if normalized <= 0:
        raise GatewayValidationError("skillId must be a positive integer for FETCH_SKILL step.")

    return normalized


def normalize_cursor(cursor: int | float | str | None) -> int:
    if cursor is None:
        return 0

    normalized = _normalize_int(cursor)
    if normalized < 0:
        raise GatewayValidationError("cursor must be greater than or equal to 0.")

    return normalized


def normalize_chunk_size(chunk_size: int | float | str | None) -> int:
    if chunk_size is None:
        return 3000

    normalized = _normalize_int(chunk_size)

    if normalized <= 0:
        raise GatewayValidationError("chunkSize must be a positive integer.")

    if normalized > 20000:
        raise GatewayValidationError("chunkSize must be less than or equal to 20000.")

    return normalized


def normalize_written_length(written_length: int | float | str | None) -> int:
    normalized = _normalize_int(written_length)
    if normalized < 0:
        raise GatewayValidationError("writtenLength must be greater than or equal to 0.")

    return normalized


def normalize_written_sha256(written_sha256: str | None) -> str:
    if written_sha256 is None:
        raise GatewayValidationError("writtenSha256 is required for VERIFY_SKILL step.")

    normalized = written_sha256.strip().lower()
    if not SHA256_PATTERN.fullmatch(normalized):
        raise GatewayValidationError("writtenSha256 must be a 64-char hex string.")

    return normalized


def normalize_customization_applied(customization_applied: bool | None) -> bool:
    if customization_applied is None:
        return False

    if not isinstance(customization_applied, bool):
        raise GatewayValidationError("customizationApplied must be a boolean.")

    return customization_applied


def _normalize_int(value: int | float | str | None) -> int:
    if value is None:
        raise GatewayValidationError("numeric value is required.")

    if isinstance(value, bool):
        raise GatewayValidationError("boolean is not allowed for numeric value.")

    if isinstance(value, int):
        return value

    if isinstance(value, float):
        if value.is_integer():
            return int(value)
        raise GatewayValidationError("numeric value must be an integer.")

    if isinstance(value, str):
        normalized = value.strip()
        if not normalized:
            raise GatewayValidationError("numeric string must not be blank.")
        if normalized.lstrip("-").isdigit():
            return int(normalized)
        raise GatewayValidationError("numeric string must be an integer format.")

    raise GatewayValidationError("numeric value must be int, float, or numeric string.")
