import os
from dataclasses import dataclass


@dataclass(frozen=True)
class GatewaySettings:
    transport: str
    host: str
    port: int
    path: str
    spring_base_url: str
    timeout_seconds: float
    mcp_personal_token: str | None

    @staticmethod
    def from_env() -> "GatewaySettings":
        transport = _normalize_transport(os.getenv("MCP_GATEWAY_TRANSPORT", "stdio"))
        host = os.getenv("MCP_GATEWAY_HOST", "0.0.0.0").strip() or "0.0.0.0"
        port = _parse_port(os.getenv("MCP_GATEWAY_PORT", "9000"))
        path = _normalize_path(os.getenv("MCP_GATEWAY_PATH", "/mcp"))
        base_url = os.getenv("SPRING_API_BASE_URL", "http://localhost:8080").rstrip("/")
        raw_timeout_seconds = os.getenv("GATEWAY_HTTP_TIMEOUT_SECONDS", "10")
        try:
            timeout_seconds = float(raw_timeout_seconds)
        except ValueError as exc:
            raise ValueError(
                "Invalid value for GATEWAY_HTTP_TIMEOUT_SECONDS: "
                f"{raw_timeout_seconds!r}. Expected a numeric value."
            ) from exc
        raw_token = os.getenv("MCP_PERSONAL_TOKEN")
        mcp_personal_token = raw_token.strip() if raw_token and raw_token.strip() else None

        return GatewaySettings(
            transport=transport,
            host=host,
            port=port,
            path=path,
            spring_base_url=base_url,
            timeout_seconds=timeout_seconds,
            mcp_personal_token=mcp_personal_token,
        )


def _normalize_transport(raw_transport: str) -> str:
    normalized = raw_transport.strip().lower()
    if normalized not in {"stdio", "streamable-http", "http"}:
        raise ValueError(
            "Invalid value for MCP_GATEWAY_TRANSPORT: "
            f"{raw_transport!r}. Expected one of: stdio, streamable-http, http."
        )

    if normalized == "http":
        return "streamable-http"

    return normalized


def _parse_port(raw_port: str) -> int:
    try:
        port = int(raw_port)
    except ValueError as exc:
        raise ValueError(
            "Invalid value for MCP_GATEWAY_PORT: "
            f"{raw_port!r}. Expected an integer between 1 and 65535."
        ) from exc

    if port < 1 or port > 65535:
        raise ValueError(
            "Invalid value for MCP_GATEWAY_PORT: "
            f"{raw_port!r}. Expected an integer between 1 and 65535."
        )

    return port


def _normalize_path(raw_path: str) -> str:
    stripped = raw_path.strip()
    if not stripped:
        return "/mcp"

    return stripped if stripped.startswith("/") else f"/{stripped}"
