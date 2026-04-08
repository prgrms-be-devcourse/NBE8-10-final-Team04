"""
test_notify.py
"""
import os
from unittest.mock import patch, MagicMock

import pytest

from collectors.scripts.shared.notify import (
    mask_sensitive,
    send_embed,
    send_error,
)


def test_mask_sensitive() -> None:
    text = "secret_key=123456"
    masked = mask_sensitive(text, ["123456"])
    assert masked == "secret_key=***MASKED***"


@patch("collectors.scripts.shared.notify.DISCORD_WEBHOOK_URL", "http://test-webhook")
@patch("collectors.scripts.shared.notify.httpx.Client")
def test_send_embed_success(mock_client_class: MagicMock) -> None:
    # Context Manager(with 구문) 모킹 설정
    mock_client = MagicMock()
    mock_response = MagicMock()
    mock_response.status_code = 204
    mock_client.post.return_value = mock_response
    mock_client_class.return_value.__enter__.return_value = mock_client

    send_embed({"test": "data"})
    mock_client.post.assert_called_once_with("http://test-webhook", json={"test": "data"})


@patch("collectors.scripts.shared.notify.DISCORD_WEBHOOK_URL", "")
@patch("collectors.scripts.shared.notify.httpx.Client")
def test_send_embed_no_webhook(mock_client_class: MagicMock) -> None:
    send_embed({"test": "data"})
    mock_client_class.assert_not_called()


@patch("collectors.scripts.shared.notify.send_embed")
def test_send_error(mock_send_embed: MagicMock) -> None:
    try:
        raise ValueError("test error")
    except ValueError as e:
        send_error("Test Error", e)

    mock_send_embed.assert_called_once()
    args, kwargs = mock_send_embed.call_args
    payload = args[0]
    embeds = payload["embeds"]

    assert len(embeds) == 1
    assert "Test Error" in embeds[0]["title"]
    assert "ValueError: test error" in embeds[0]["description"]