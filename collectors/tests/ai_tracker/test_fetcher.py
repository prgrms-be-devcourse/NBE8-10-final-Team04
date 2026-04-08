"""
tests/ai_tracker/test_fetcher.py
"""

from unittest.mock import patch, MagicMock
from bs4 import BeautifulSoup
import httpx
import pytest

from collectors.scripts.ai_tracker.fetcher import (
    extract_text_from_soup,
    fetch_scrape_html,
    fetch_detail_content
)

def test_extract_text_from_soup() -> None:
    """불필요한 태그(script, style 등)가 제거되고 순수 텍스트만 추출되는지 검증 [cite: 1]"""
    raw_html: str = """
    <html>
        <head><style>.css { color: red; }</style></head>
        <body>
            <nav>Menu</nav>
            <main>
                <h2>Real Content</h2>
                <script>alert('js');</script>
                <p>Detail text</p>
            </main>
            <footer>Copyright</footer>
        </body>
    </html>
    """
    soup = BeautifulSoup(raw_html, "html.parser")
    result: str = extract_text_from_soup(soup)

    assert "Real Content" in result
    assert "Detail text" in result
    assert "Menu" not in result
    assert "alert('js')" not in result

@patch("collectors.scripts.ai_tracker.fetcher.fetch_with_cf_br")
@patch("httpx.Client.get")
def test_fetch_detail_content_403_fallback(mock_get: MagicMock, mock_cf_br: MagicMock) -> None:
    """상세 수집 중 HTTP 403 발생 시 CF BR로 폴백되어야 함 [cite: 1]"""
    # 403 상태의 Mock 응답 설정
    mock_resp = MagicMock()
    mock_resp.status_code = 403
    mock_get.return_value = mock_resp

    mock_cf_br.return_value = "CF BR Content"

    result: str = fetch_detail_content("https://test.com/detail", "OpenAI")

    assert result == "CF BR Content"
    mock_cf_br.assert_called_once_with("https://test.com/detail", extract_text=True)

@patch("collectors.scripts.ai_tracker.fetcher.fetch_with_cf_br")
def test_fetch_detail_openai_index_direct_cf_br(mock_cf_br: MagicMock) -> None:
    """OpenAI index 페이지는 httpx를 타지 않고 즉시 CF BR로 우회해야 함 [cite: 1]"""
    mock_cf_br.return_value = "OpenAI Direct CF BR"

    result: str = fetch_detail_content("https://openai.com/index/something", "OpenAI")

    assert result == "OpenAI Direct CF BR"
    mock_cf_br.assert_called_once_with("https://openai.com/index/something", extract_text=True)