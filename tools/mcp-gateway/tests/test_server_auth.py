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
    def __init__(self, *_args, **_kwargs):
        pass

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

    def test_resolve_token_raises_when_both_header_and_env_token_missing(self):
        ctx = SimpleNamespace(request_context=SimpleNamespace(request=SimpleNamespace(headers={})))
        with mock.patch.object(server, "settings", _build_settings(None)):
            with self.assertRaises(GatewayValidationError):
                server._resolve_mcp_personal_token(ctx)


def _build_settings(token: str | None) -> GatewaySettings:
    return GatewaySettings(
        transport="stdio",
        host="0.0.0.0",
        port=9000,
        path="/mcp",
        spring_base_url="http://localhost:8080",
        timeout_seconds=10.0,
        mcp_personal_token=token,
    )


if __name__ == "__main__":
    unittest.main()
