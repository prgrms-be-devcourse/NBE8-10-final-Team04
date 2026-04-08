"""
tests/ai_tracker/test_models.py
"""

from typing import Any
from datetime import datetime, timezone
from collectors.scripts.ai_tracker.models import make_id, parse_date, make_item

def test_make_id_rss_strategy() -> None:
    """RSS는 URL을 기준으로 ID가 생성되어야 함 [cite: 1]"""
    id1: str = make_id("OpenAI", "rss", "제목 다름 1", "https://same-url.com")
    id2: str = make_id("OpenAI", "rss", "제목 다름 2", "https://same-url.com")

    assert id1 == id2
    assert len(id1) == 16

def test_make_id_scrape_strategy() -> None:
    """Scrape는 제목(Title)을 기준으로 ID가 생성되어야 함 [cite: 1]"""
    id1: str = make_id("OpenAI", "scrape", "동일한 제목", "https://url-1.com")
    id2: str = make_id("OpenAI", "scrape", "동일한 제목", "https://url-2.com")

    assert id1 == id2
    assert len(id1) == 16

def test_parse_date_valid() -> None:
    """정상적인 날짜 문자열 파싱 검증 [cite: 1]"""
    parsed: str = parse_date("2026-04-08T09:00:00Z")
    assert "2026-04-08T09:00:00+00:00" == parsed

def test_parse_date_invalid_fallback() -> None:
    """잘못된 날짜 형식이거나 None일 경우 현재 UTC 시각을 반환해야 함 [cite: 1]"""
    parsed_invalid: str = parse_date("invalid-date")
    parsed_none: str = parse_date(None)

    # ISO 8601 포맷 검증
    assert parsed_invalid.endswith("+00:00")
    assert parsed_none.endswith("+00:00")

def test_make_item_structure() -> None:
    """make_item이 올바른 딕셔너리 구조를 반환하는지 검증 [cite: 1]"""
    source: dict[str, Any] = {"provider": "Anthropic", "label": "Anthropic 뉴스"}
    item: dict[str, Any] = make_item(
        source=source,
        title="Test Title",
        url="https://test.com",
        summary="Test Summary",
        stype="scrape",
        pub_at="2026-04-08T00:00:00Z"
    )

    assert item["provider"] == "Anthropic"
    assert item["source_type"] == "scrape"
    assert item["title"] == "Test Title"
    assert item["raw_content"] == "" # 초기값 검증
    assert "2026-04-08" in item["published_at"]