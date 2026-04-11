"""
ai_tracker/parsers.py — 소스별 HTML 파서

각 수집 소스의 HTML 구조에 맞는 파서 함수 모음.
새 소스 추가 시 파서 함수를 작성하고 PARSERS 딕셔너리에 등록.
"""

import json
import logging
from typing import Any

from bs4 import BeautifulSoup

from collectors.scripts.ai_tracker.config import EXCLUDE_NAV_TEXTS, MAX_ITEMS_PER_SOURCE
from collectors.scripts.ai_tracker.models import make_item

log: logging.Logger = logging.getLogger(__name__)


def parse_generic_changelog(html: str, source: dict[str, Any]) -> list[dict[str, Any]]:
    """
    구조 변경 대비 범용 파서 (휴리스틱 탐색).
    특정 CSS 클래스에 의존하지 않고 HTML 시맨틱(H2, H3 및 형제 요소)을 기반으로 업데이트 내역을 추출합니다.
    어떤 소스(provider)에서든 구조가 깨졌을 때 재사용 가능한 백업 파서입니다.
    """
    soup = BeautifulSoup(html, "html.parser")
    items: list[dict[str, Any]] = []

    # 네비게이션을 피해 실제 내용이 있을 만한 영역 특정
    main_area = soup.find("main") or soup.find("article") or soup.find("div", id="root") or soup
    # 소스에서 provider 이름 추출 (없을 경우 기본값)
    provider: str = source.get("provider", "Update")

    for heading in main_area.select("h2, h3"):
        if len(items) >= MAX_ITEMS_PER_SOURCE:
            break

        heading_text: str = heading.get_text(strip=True)
        if len(heading_text) < 4 or heading_text in EXCLUDE_NAV_TEXTS:
            continue

        content_parts: list[str] = []
        sib = heading.find_next_sibling()

        while sib and sib.name not in ("h1", "h2", "h3"):
            if sib.name == "p":
                text: str = sib.get_text(strip=True)
                if text:
                    content_parts.append(text)
            elif sib.name == "ul":
                content_parts.extend(
                    li.get_text(strip=True) for li in sib.find_all("li", recursive=False)
                )
            sib = sib.find_next_sibling()

        if content_parts:
            summary_str: str = " ".join(content_parts)[:200] # 요약용은 짧게
            raw_content: str = "\n".join(f"- {part}" for part in content_parts)

            title: str = f"{provider} Update ({heading_text}): {summary_str[:20]}..."

            item = make_item(source, title, source["url"], summary_str, "scrape", heading_text)
            item["raw_content"] = raw_content
            items.append(item)

    return items


def parse_anthropic_news(html: str, source: dict[str, Any]) -> list[dict[str, Any]]:
    """Anthropic 뉴스 페이지 파싱. 상위 MAX_ITEMS_PER_SOURCE개 항목 반환."""
    soup = BeautifulSoup(html, "html.parser")
    items: list[dict[str, Any]] = []
    main_area = soup.find("main") or soup
    for li in main_area.select("li")[: MAX_ITEMS_PER_SOURCE * 5]:
        anchor = li.find("a", href=True)
        if not anchor:
            continue
        href: str = anchor["href"]
        if not href.startswith(("/news/", "/mars", "/research/")):
            continue
        title: str = anchor.get_text(strip=True)
        if title in EXCLUDE_NAV_TEXTS:
            continue
        url: str = (
            f"https://www.anthropic.com{href}" if href.startswith("/") else href
        )
        p_tag = li.find("p")
        summary: str = p_tag.get_text(strip=True) if p_tag else title
        items.append(make_item(source, title, url, summary, "scrape"))

        if len(items) >= MAX_ITEMS_PER_SOURCE:
            break

    return items


def parse_openai_changelog(html: str, source: dict[str, Any]) -> list[dict[str, Any]]:
    """OpenAI Platform Changelog 파싱."""
    if html.strip().startswith("{"):
        try:
            html = json.loads(html).get("result", html)
        except Exception as e:
            log.warning("[openai_changelog] JSON 언래핑 실패: %s", e)

    items = _parse_openai_primary(html, source)
    if not items:
        log.warning("[openai_changelog] primary 파싱 결과 없음 — 범용 파서(fallback) 시도")
        items = parse_generic_changelog(html, source)
    if not items:
        log.error("[openai_changelog] 범용 파서도 결과 없음 — 페이지 구조 변경 가능성 높음")
    return items


def _parse_openai_primary(html: str, source: dict[str, Any]) -> list[dict[str, Any]]:
    """MarkdownContent div 기반 파싱."""
    soup = BeautifulSoup(html, "html.parser")
    items: list[dict[str, Any]] = []

    for div in soup.find_all("div", class_=lambda c: c and "MarkdownContent" in c):
        if len(items) >= MAX_ITEMS_PER_SOURCE:
            break

        h_tag = div.find(["h2", "h3"])
        date_text: str = h_tag.get_text(strip=True) if h_tag else ""

        p_tag = div.find("p")
        summary: str = p_tag.get_text(strip=True) if p_tag else "업데이트 내역"

        title_prefix = f"OpenAI Update ({date_text})" if date_text else "OpenAI Update"
        title: str = f"{title_prefix}: {summary[:20]}..."

        raw_content: str = div.get_text(separator="\n", strip=True)

        item = make_item(source, title, source["url"], summary, "scrape")
        item["raw_content"] = raw_content
        items.append(item)

    return items


def parse_anthropic_changelog(html: str, source: dict[str, Any]) -> list[dict[str, Any]]:
    """Anthropic API Changelog 파싱."""
    soup = BeautifulSoup(html, "html.parser")
    items: list[dict[str, Any]] = []

    for heading in soup.select("h2, h3"):
        if len(items) >= MAX_ITEMS_PER_SOURCE:
            break

        date_text: str = heading.get_text(strip=True)

        if date_text in EXCLUDE_NAV_TEXTS or not date_text:
            continue

        words = date_text.split()
        if len(words) == 1 and not any(ch.isdigit() for ch in date_text):
            continue

        content_parts: list[str] = []
        sib = heading.find_next_sibling()
        while sib and sib.name not in ("h2", "h3"):
            if sib.name == "ul":
                content_parts.extend(
                    li.get_text(strip=True)
                    for li in sib.find_all("li", recursive=False)
                )
            elif sib.name == "p":
                text = sib.get_text(strip=True)
                if text:
                    content_parts.append(text)
            sib = sib.find_next_sibling()

        if not content_parts:
            continue

        title: str = f"Anthropic Release: {date_text}"
        summary_str: str = "\n".join(f"- {part}" for part in content_parts)
        raw_content: str = summary_str

        item = make_item(source, title, source["url"], summary_str, "scrape", date_text)
        item["raw_content"] = raw_content
        items.append(item)

    return items


def parse_google_changelog(html: str, source: dict[str, Any]) -> list[dict[str, Any]]:
    """Gemini API Changelog 파싱."""
    if html.strip().startswith("{"):
        try:
            html = json.loads(html).get("result", html)
        except Exception as e:
            log.warning("[google_changelog] JSON 언래핑 실패: %s", e)

    soup = BeautifulSoup(html, "html.parser")
    items: list[dict[str, Any]] = []

    for h2 in soup.select("h2"):
        if len(items) >= MAX_ITEMS_PER_SOURCE:
            break

        date_text: str = h2.get_text(strip=True)
        sib = h2.find_next_sibling()
        if not sib or sib.name != "ul":
            continue

        bullets = [li.get_text(strip=True) for li in sib.find_all("li", recursive=False)]
        if not bullets:
            continue

        title: str = f"Gemini API Update: {date_text}"
        summary_str: str = "\n".join(f"- {b}" for b in bullets)
        raw_content: str = summary_str

        item = make_item(source, title, source["url"], summary_str, "scrape", date_text)
        item["raw_content"] = raw_content
        items.append(item)

    return items


# 파서 키 → 함수 매핑. 새 소스 추가 시 여기에 등록.
PARSERS: dict[str, Any] = {
    "openai_changelog":    parse_openai_changelog,
    "anthropic_news":      parse_anthropic_news,
    "anthropic_changelog": parse_anthropic_changelog,
    "google_changelog":    parse_google_changelog,
}