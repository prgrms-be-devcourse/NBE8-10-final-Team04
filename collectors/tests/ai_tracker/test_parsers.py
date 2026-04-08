"""
tests/ai_tracker/test_parsers.py
"""

from typing import Any
from collectors.scripts.ai_tracker.parsers import parse_generic_changelog
import collectors.scripts.ai_tracker.parsers as parsers_mod

def test_parse_generic_changelog_heuristic() -> None:
    """시맨틱 태그(h2, h3) 기반 범용 파서가 정상적으로 작동하는지 검증"""
    # 테스트 전역 변수 오버라이드
    parsers_mod.MAX_ITEMS_PER_SOURCE = 2

    dummy_html: str = """
    <main>
        <h2>2026-04-08</h2>
        <p>First summary line.</p>
        <ul><li>Feature A</li><li>Feature B</li></ul>
        
        <h2>2026-04-07</h2>
        <p>Second summary line.</p>
        
        <h2>2026-04-06</h2>
        <p>This should be ignored due to MAX_ITEMS.</p>
    </main>
    """

    source: dict[str, Any] = {
        "provider": "TestVendor",
        "url": "https://test.com/changelog",
        "label": "Test Label"
    }

    items: list[dict[str, Any]] = parse_generic_changelog(dummy_html, source)

    assert len(items) == 2 # MAX_ITEMS_PER_SOURCE 제한 동작 확인

    # 동적 Provider 제목 주입 확인
    assert items[0]['title'].startswith("TestVendor Update (2026-04-08):")
    # raw_content 구성 확인
    assert "- First summary line." in items[0]["raw_content"]
    assert "- Feature A" in items[0]["raw_content"]