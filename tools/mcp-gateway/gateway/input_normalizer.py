from __future__ import annotations

SUPPORTED_AGENT_TYPES = {"CLAUDE", "CODEX", "GEMINI"}
SUPPORTED_FLOW_STEPS = ("START", "COLLECTED")


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


def normalize_user_input_confirmed(user_input_confirmed: bool | None) -> bool:
    if user_input_confirmed is not True:
        raise GatewayValidationError(
            "userInputConfirmed must be true for COLLECTED step."
        )

    return True
