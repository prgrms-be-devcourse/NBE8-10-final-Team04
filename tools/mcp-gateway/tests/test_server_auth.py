import importlib
import sys
import types
import unittest
from types import SimpleNamespace
from unittest import mock

from gateway.input_normalizer import GatewayValidationError
from gateway.settings import GatewaySettings


class _FakeContext:
    pass


class _FakeFastMCP:
    last_init_args = None
    last_init_kwargs = None

    def __init__(self, *_args, **_kwargs):
        _FakeFastMCP.last_init_args = _args
        _FakeFastMCP.last_init_kwargs = _kwargs

    def tool(self, **_kwargs):
        def decorator(func):
            return func

        return decorator

    def run(self, **_kwargs):
        return None


_fake_fastmcp_module = types.ModuleType("mcp.server.fastmcp")
_fake_fastmcp_module.Context = _FakeContext
_fake_fastmcp_module.FastMCP = _FakeFastMCP

_fake_server_module = types.ModuleType("mcp.server")
_fake_server_module.fastmcp = _fake_fastmcp_module

_fake_mcp_module = types.ModuleType("mcp")
_fake_mcp_module.server = _fake_server_module

with mock.patch.dict(
    sys.modules,
    {
        "mcp": _fake_mcp_module,
        "mcp.server": _fake_server_module,
        "mcp.server.fastmcp": _fake_fastmcp_module,
    },
):
    server = importlib.import_module("server")


def _build_ctx_with_authorization(header_value: str):
    request = SimpleNamespace(headers={"authorization": header_value})
    request_context = SimpleNamespace(request=request)
    return SimpleNamespace(request_context=request_context)


def _build_ctx_with_query_token(token_value: str):
    request = SimpleNamespace(
        headers={},
        query_params={"token": token_value},
        url=f"https://api.han-minhee.site/mcp?token={token_value}",
    )
    request_context = SimpleNamespace(request=request)
    return SimpleNamespace(request_context=request_context)


class ServerAuthTokenResolutionTest(unittest.TestCase):
    def test_resolve_token_from_authorization_header(self):
        ctx = _build_ctx_with_authorization("Bearer mcp_token_header_1")
        with mock.patch.object(server, "settings", _build_settings(None)):
            token = server._resolve_mcp_personal_token(ctx)

        self.assertEqual(token, "mcp_token_header_1")

    def test_resolve_token_raises_when_authorization_is_malformed(self):
        ctx = _build_ctx_with_authorization("Basic abc")
        with mock.patch.object(server, "settings", _build_settings(None)):
            with self.assertRaises(GatewayValidationError):
                server._resolve_mcp_personal_token(ctx)

    def test_resolve_token_uses_env_fallback_when_header_missing(self):
        ctx = SimpleNamespace(request_context=SimpleNamespace(request=SimpleNamespace(headers={})))
        with mock.patch.object(server, "settings", _build_settings("env_token_123")):
            token = server._resolve_mcp_personal_token(ctx)

        self.assertEqual(token, "env_token_123")

    def test_resolve_token_from_query_parameter_when_header_missing(self):
        ctx = _build_ctx_with_query_token("mcp_query_token_777")
        with mock.patch.object(server, "settings", _build_settings(None)):
            token = server._resolve_mcp_personal_token(ctx)

        self.assertEqual(token, "mcp_query_token_777")

    def test_resolve_token_prefers_authorization_header_over_query_parameter(self):
        ctx = _build_ctx_with_query_token("mcp_query_token_777")
        ctx.request_context.request.headers = {"authorization": "Bearer mcp_header_token_999"}

        with mock.patch.object(server, "settings", _build_settings(None)):
            token = server._resolve_mcp_personal_token(ctx)

        self.assertEqual(token, "mcp_header_token_999")

    def test_resolve_token_raises_when_both_header_and_env_token_missing(self):
        ctx = SimpleNamespace(request_context=SimpleNamespace(request=SimpleNamespace(headers={})))
        with mock.patch.object(server, "settings", _build_settings(None)):
            with self.assertRaises(GatewayValidationError):
                server._resolve_mcp_personal_token(ctx)


class _FakeMcpVarKeywordRun:
    def run(self, **_kwargs):
        return None


class _FakeMcpTransportOnlyRun:
    def run(self, transport):
        return transport


class _FakeMcpInspectFallbackRun:
    def __init__(self):
        self.calls: list[dict[str, object]] = []

    def run(self, **kwargs):
        self.calls.append(dict(kwargs))
        if "host" in kwargs or "port" in kwargs or "path" in kwargs:
            raise TypeError("unexpected kwargs for this SDK")
        return None


class _FakeMcpWithSettings:
    def __init__(self):
        self.settings = SimpleNamespace(host=None, port=None, streamable_http_path=None)

    def run(self, **_kwargs):
        return None


class ServerStreamableHttpRunCompatibilityTest(unittest.TestCase):
    def test_create_mcp_server_uses_gateway_settings_in_constructor(self):
        fake_constructor = mock.Mock(return_value=_FakeMcpVarKeywordRun())
        with mock.patch.object(server, "FastMCP", fake_constructor):
            created = server._create_mcp_server(
                _build_settings(None, host="0.0.0.0", port=9000, path="/mcp")
            )

        self.assertEqual(created, fake_constructor.return_value)
        fake_constructor.assert_called_once_with(
            "start-ai-mcp-gateway",
            host="0.0.0.0",
            port=9000,
            streamable_http_path="/mcp",
        )

    def test_resolve_streamable_http_run_kwargs_supports_var_keyword_signature(self):
        with mock.patch.object(server, "mcp", _FakeMcpVarKeywordRun()), \
                mock.patch.object(server, "settings", _build_settings(None, host="127.0.0.1", port=9100, path="/mcp")):
            run_kwargs, signature_inspected = server._resolve_streamable_http_run_kwargs()

        self.assertTrue(signature_inspected)
        self.assertEqual(
            run_kwargs,
            {
                "transport": "streamable-http",
                "host": "127.0.0.1",
                "port": 9100,
                "path": "/mcp",
            },
        )

    def test_resolve_streamable_http_run_kwargs_filters_for_explicit_signature(self):
        with mock.patch.object(server, "mcp", _FakeMcpTransportOnlyRun()), \
                mock.patch.object(server, "settings", _build_settings(None, host="127.0.0.1", port=9100, path="/mcp")):
            run_kwargs, signature_inspected = server._resolve_streamable_http_run_kwargs()

        self.assertTrue(signature_inspected)
        self.assertEqual(run_kwargs, {"transport": "streamable-http"})

    def test_resolve_streamable_http_run_kwargs_falls_back_when_signature_introspection_fails(self):
        with mock.patch.object(server, "mcp", _FakeMcpVarKeywordRun()), \
                mock.patch.object(server, "settings", _build_settings(None, host="127.0.0.1", port=9100, path="/mcp")), \
                mock.patch.object(server.inspect, "signature", side_effect=ValueError("unsupported")):
            run_kwargs, signature_inspected = server._resolve_streamable_http_run_kwargs()

        self.assertFalse(signature_inspected)
        self.assertEqual(
            run_kwargs,
            {
                "transport": "streamable-http",
                "host": "127.0.0.1",
                "port": 9100,
                "path": "/mcp",
            },
        )

    def test_run_streamable_http_retries_with_transport_only_when_signature_unknown(self):
        fake_mcp = _FakeMcpInspectFallbackRun()
        with mock.patch.object(server, "mcp", fake_mcp), \
                mock.patch.object(server, "settings", _build_settings(None, host="127.0.0.1", port=9100, path="/mcp")), \
                mock.patch.object(server.inspect, "signature", side_effect=TypeError("cannot inspect")):
            server._run_streamable_http()

        self.assertEqual(len(fake_mcp.calls), 2)
        self.assertEqual(
            fake_mcp.calls[0],
            {
                "transport": "streamable-http",
                "host": "127.0.0.1",
                "port": 9100,
                "path": "/mcp",
            },
        )
        self.assertEqual(fake_mcp.calls[1], {"transport": "streamable-http"})

    def test_apply_streamable_http_settings_updates_available_fields(self):
        fake_mcp = _FakeMcpWithSettings()
        with mock.patch.object(server, "mcp", fake_mcp), \
                mock.patch.object(server, "settings", _build_settings(None, host="127.0.0.1", port=9100, path="/gateway")):
            server._apply_streamable_http_settings()

        self.assertEqual(fake_mcp.settings.host, "127.0.0.1")
        self.assertEqual(fake_mcp.settings.port, 9100)
        self.assertEqual(fake_mcp.settings.streamable_http_path, "/gateway")


def _build_settings(
        token: str | None,
        *,
        host: str = "0.0.0.0",
        port: int = 9000,
        path: str = "/mcp",
) -> GatewaySettings:
    return GatewaySettings(
        transport="stdio",
        host=host,
        port=port,
        path=path,
        spring_base_url="http://localhost:8080",
        timeout_seconds=10.0,
        mcp_personal_token=token,
    )


if __name__ == "__main__":
    unittest.main()
