from __future__ import annotations

import inspect
from typing import Any
from urllib.parse import parse_qs, urlparse

from mcp.server.fastmcp import Context, FastMCP

from gateway.auto_flow_service import AutoFlowService
from gateway.input_normalizer import GatewayValidationError
from gateway.settings import GatewaySettings
from gateway.spring_proxy_client import (
    GatewayConnectionError,
    GatewayHttpError,
    SpringProxyClient,
)

settings = GatewaySettings.from_env()


def _create_mcp_server(gateway_settings: GatewaySettings) -> FastMCP:
    return FastMCP(
        "start-ai-mcp-gateway",
        host=gateway_settings.host,
        port=gateway_settings.port,
        streamable_http_path=gateway_settings.path,
    )


mcp = _create_mcp_server(settings)
client = SpringProxyClient(
    spring_base_url=settings.spring_base_url,
    timeout_seconds=settings.timeout_seconds,
    default_mcp_personal_token=settings.mcp_personal_token,
)
auto_flow_service = AutoFlowService(client)

def _error(error_type: str, message: str, **extra: Any) -> dict[str, Any]:
    payload: dict[str, Any] = {
        "success": False,
        "errorType": error_type,
        "message": message,
    }
    payload.update(extra)
    return payload


def _success(payload: Any) -> dict[str, Any]:
    if isinstance(payload, dict):
        response = dict(payload)
    else:
        response = {"data": payload}

    response["success"] = True
    return response


def _extract_authorization_header(ctx: Context | None) -> str | None:
    if ctx is None:
        return None

    request_context = getattr(ctx, "request_context", None)
    request = getattr(request_context, "request", None)
    headers = getattr(request, "headers", None)
    if headers is None:
        return None

    if hasattr(headers, "get"):
        header = headers.get("authorization")
        if header:
            return str(header)

        header = headers.get("Authorization")
        if header:
            return str(header)

    return None


def _extract_query_token(ctx: Context | None) -> str | None:
    if ctx is None:
        return None

    request_context = getattr(ctx, "request_context", None)
    request = getattr(request_context, "request", None)
    if request is None:
        return None

    query_params = getattr(request, "query_params", None)
    query_token = _read_query_token_from_mapping(query_params)
    if query_token:
        return query_token

    url = getattr(request, "url", None)
    if url is None:
        return None

    parsed_query = parse_qs(urlparse(str(url)).query)
    return _read_query_token_from_mapping(parsed_query)


def _read_query_token_from_mapping(mapping: Any) -> str | None:
    if mapping is None or not hasattr(mapping, "get"):
        return None

    for key in ("token", "mcp_token", "mcpPersonalToken"):
        raw_value = mapping.get(key)
        if raw_value is None:
            continue

        value = raw_value[0] if isinstance(raw_value, list) else raw_value
        normalized = str(value).strip()
        if normalized:
            return normalized

    return None


def _resolve_mcp_personal_token(ctx: Context | None) -> str:
    authorization_header = _extract_authorization_header(ctx)
    if authorization_header:
        normalized = authorization_header.strip()
        if normalized.lower().startswith("bearer "):
            token = normalized[7:].strip()
            if token:
                return token

        raise GatewayValidationError("Authorization header must be in the format: Bearer <mcp_token>.")

    query_token = _extract_query_token(ctx)
    if query_token:
        return query_token

    if settings.mcp_personal_token:
        return settings.mcp_personal_token

    raise GatewayValidationError(
        "MCP personal token is required. Provide Authorization: Bearer <mcp_token>, "
        "append ?token=<mcp_token> to the MCP URL, or set MCP_PERSONAL_TOKEN."
    )


def _apply_streamable_http_settings() -> None:
    if not hasattr(mcp, "settings"):
        return

    # Keep compatibility across MCP SDK versions:
    # some versions read network/path from mcp.settings, while run()
    # may not accept host/port/path kwargs directly.
    if hasattr(mcp.settings, "host"):
        mcp.settings.host = settings.host
    if hasattr(mcp.settings, "port"):
        mcp.settings.port = settings.port
    if hasattr(mcp.settings, "streamable_http_path"):
        mcp.settings.streamable_http_path = settings.path
    elif hasattr(mcp.settings, "mount_path"):
        mcp.settings.mount_path = settings.path


def _resolve_streamable_http_run_kwargs() -> tuple[dict[str, Any], bool]:
    desired_run_kwargs: dict[str, Any] = {
        "transport": "streamable-http",
        "host": settings.host,
        "port": settings.port,
        "path": settings.path,
    }

    try:
        run_signature = inspect.signature(mcp.run)
    except (TypeError, ValueError):
        return dict(desired_run_kwargs), False

    accepts_var_keyword = any(
        parameter.kind == inspect.Parameter.VAR_KEYWORD
        for parameter in run_signature.parameters.values()
    )
    if accepts_var_keyword:
        return dict(desired_run_kwargs), True

    filtered_kwargs = {
        key: value
        for key, value in desired_run_kwargs.items()
        if key in run_signature.parameters
    }
    return filtered_kwargs, True


def _run_streamable_http() -> None:
    _apply_streamable_http_settings()
    run_kwargs, signature_inspected = _resolve_streamable_http_run_kwargs()

    try:
        mcp.run(**run_kwargs)
    except TypeError:
        # If inspect failed, attempt a minimal fallback path instead of crashing
        # due to kwargs mismatch across unknown SDK versions.
        if signature_inspected:
            raise

        try:
            mcp.run(transport="streamable-http")
        except TypeError:
            mcp.run()


@mcp.tool(name="get_start_agent_template")
def get_start_agent_template(agentType: str, ctx: Context | None = None) -> dict[str, Any]:
    """Fetches start.agent.md template. Token is resolved from Authorization header (or env fallback)."""
    try:
        mcp_personal_token = _resolve_mcp_personal_token(ctx)
        response = client.get_start_agent_template(
            mcp_personal_token=mcp_personal_token,
            agent_type=agentType,
        )
        return _success(response)
    except GatewayValidationError as validation_error:
        return _error("validation_error", str(validation_error))
    except GatewayHttpError as http_error:
        return _error(
            "spring_http_error",
            "Failed to fetch start-agent template from Spring API.",
            statusCode=http_error.status_code,
            responseBody=http_error.response_body,
        )
    except GatewayConnectionError as connection_error:
        return _error("connection_error", str(connection_error))


@mcp.tool(name="recommend_skills")
def recommend_skills(queries: list[str], ctx: Context | None = None) -> dict[str, Any]:
    """Fetches ranked skills recommendation. Token is resolved from Authorization header (or env fallback)."""
    try:
        mcp_personal_token = _resolve_mcp_personal_token(ctx)
        response = client.recommend_skills(
            mcp_personal_token=mcp_personal_token,
            queries=queries,
        )
        return _success(response)
    except GatewayValidationError as validation_error:
        return _error("validation_error", str(validation_error))
    except GatewayHttpError as http_error:
        return _error(
            "spring_http_error",
            "Failed to fetch recommendations from Spring API.",
            statusCode=http_error.status_code,
            responseBody=http_error.response_body,
        )
    except GatewayConnectionError as connection_error:
        return _error("connection_error", str(connection_error))


@mcp.tool(name="start_auto_flow")
def start_auto_flow(
        step: str = "START",
        agentType: str | None = None,
        flowId: str | None = None,
        queries: list[str] | None = None,
        userInputConfirmed: bool | None = None,
        ctx: Context | None = None,
) -> dict[str, Any]:
    """
    Runs single-tool auto flow in runner mode:
    - START: fetch start.agent template and return write action
    - COLLECTED: fetch recommendation metadata and return runner plan
    Agent type resolution:
    - request agentType if provided
    - otherwise CODEX fallback
    """
    try:
        mcp_personal_token = _resolve_mcp_personal_token(ctx)
        return auto_flow_service.run(
            step=step,
            mcp_personal_token=mcp_personal_token,
            agent_type=agentType,
            flow_id=flowId,
            queries=queries,
            user_input_confirmed=userInputConfirmed,
        )
    except GatewayValidationError as validation_error:
        return _error("validation_error", str(validation_error))
    except GatewayHttpError as http_error:
        return _error(
            "spring_http_error",
            "Failed to process start_auto_flow with Spring API.",
            statusCode=http_error.status_code,
            responseBody=http_error.response_body,
        )
    except GatewayConnectionError as connection_error:
        return _error("connection_error", str(connection_error))


if __name__ == "__main__":
    if settings.transport == "stdio":
        mcp.run(transport="stdio")
    else:
        _run_streamable_http()
