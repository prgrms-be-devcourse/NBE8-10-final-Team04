import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sqlalchemy import text
from app.db.database import SessionLocal


def get_columns(db, table_name: str) -> list[str]:
    rows = db.execute(text("""
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = :table_name
        ORDER BY ordinal_position
    """), {"table_name": table_name}).fetchall()
    return [row.column_name for row in rows]


def find_first_existing(columns: list[str], candidates: list[str]) -> str | None:
    for c in candidates:
        if c in columns:
            return c
    return None


def seed_ai_models():
    db = SessionLocal()

    try:
        vendor_columns = get_columns(db, "ai_vendors")
        family_columns = get_columns(db, "ai_model_families")
        model_columns = get_columns(db, "ai_models")

        print("ai_vendors columns:", vendor_columns)
        print("ai_model_families columns:", family_columns)
        print("ai_models columns:", model_columns)

        # -------------------------
        # 1. ai_vendors 이름 컬럼 자동 탐지
        # -------------------------
        vendor_name_col = find_first_existing(
            vendor_columns,
            ["vendor_name", "name", "display_name", "title"]
        )

        if not vendor_name_col:
            raise Exception(
                f"ai_vendors 테이블에서 이름 컬럼을 찾지 못했어. 현재 컬럼: {vendor_columns}"
            )

        vendor_desc_col = find_first_existing(
            vendor_columns,
            ["common_description", "description", "summary"]
        )

        # created_at / updated_at 존재 여부
        vendor_has_created_at = "created_at" in vendor_columns
        vendor_has_updated_at = "updated_at" in vendor_columns

        def insert_vendor_if_missing(vendor_name: str, description: str):
            insert_cols = []
            select_values = []

            if vendor_has_created_at:
                insert_cols.append("created_at")
                select_values.append("NOW()")
            if vendor_has_updated_at:
                insert_cols.append("updated_at")
                select_values.append("NOW()")

            insert_cols.append(vendor_name_col)
            select_values.append(":vendor_name")

            if vendor_desc_col:
                insert_cols.append(vendor_desc_col)
                select_values.append(":description")

            sql = f"""
                INSERT INTO ai_vendors ({", ".join(insert_cols)})
                SELECT {", ".join(select_values)}
                WHERE NOT EXISTS (
                    SELECT 1 FROM ai_vendors WHERE {vendor_name_col} = :vendor_name
                )
            """

            db.execute(text(sql), {
                "vendor_name": vendor_name,
                "description": description,
            })

        insert_vendor_if_missing("OpenAI", "OpenAI 계열 AI 제공사")
        insert_vendor_if_missing("Google", "Google 계열 AI 제공사")
        insert_vendor_if_missing("Anthropic", "Anthropic 계열 AI 제공사")

        vendor_rows = db.execute(text(f"""
            SELECT id, {vendor_name_col} AS vendor_name
            FROM ai_vendors
            WHERE {vendor_name_col} IN ('OpenAI', 'Google', 'Anthropic')
        """)).fetchall()

        vendor_map = {row.vendor_name: row.id for row in vendor_rows}

        openai_vendor_id = vendor_map["OpenAI"]
        google_vendor_id = vendor_map["Google"]
        anthropic_vendor_id = vendor_map["Anthropic"]

        # -------------------------
        # 2. ai_model_families 넣기
        # -------------------------
        family_has_created_at = "created_at" in family_columns
        family_has_updated_at = "updated_at" in family_columns
        family_has_description = "common_description" in family_columns

        def insert_family_if_missing(family_name: str, description: str, vendor_id: int):
            insert_cols = []
            select_values = []

            if family_has_created_at:
                insert_cols.append("created_at")
                select_values.append("NOW()")
            if family_has_updated_at:
                insert_cols.append("updated_at")
                select_values.append("NOW()")

            insert_cols.append("family_name")
            select_values.append(":family_name")

            if family_has_description:
                insert_cols.append("common_description")
                select_values.append(":description")

            insert_cols.append("vendor_id")
            select_values.append(":vendor_id")

            sql = f"""
                INSERT INTO ai_model_families ({", ".join(insert_cols)})
                SELECT {", ".join(select_values)}
                WHERE NOT EXISTS (
                    SELECT 1 FROM ai_model_families WHERE family_name = :family_name
                )
            """

            db.execute(text(sql), {
                "family_name": family_name,
                "description": description,
                "vendor_id": vendor_id,
            })

        insert_family_if_missing("OpenAI GPT", "OpenAI 계열 모델", openai_vendor_id)
        insert_family_if_missing("Google Gemini", "Google 계열 모델", google_vendor_id)
        insert_family_if_missing("Anthropic Claude", "Anthropic 계열 모델", anthropic_vendor_id)

        family_rows = db.execute(text("""
            SELECT id, family_name
            FROM ai_model_families
            WHERE family_name IN ('OpenAI GPT', 'Google Gemini', 'Anthropic Claude')
        """)).fetchall()

        family_map = {row.family_name: row.id for row in family_rows}

        openai_family_id = family_map["OpenAI GPT"]
        gemini_family_id = family_map["Google Gemini"]
        claude_family_id = family_map["Anthropic Claude"]

        # -------------------------
        # 3. ai_models 넣기
        # -------------------------
        def insert_model_if_missing(
            api_id: str,
            model_name: str,
            category: str,
            context_window: int,
            input_price: float,
            output_price: float,
            max_output_tokens: int,
            family_id: int,
            input_modalities_json: str,
            output_modalities_json: str,
        ):
            sql = """
                INSERT INTO ai_models (
                    created_at,
                    updated_at,
                    api_id,
                    category,
                    context_window,
                    input_price,
                    max_output_tokens,
                    model_image_url,
                    model_name,
                    output_price,
                    is_preview,
                    release_date,
                    family_id,
                    input_modalities,
                    output_modalities
                )
                SELECT
                    NOW(),
                    NOW(),
                    :api_id,
                    :category,
                    :context_window,
                    :input_price,
                    :max_output_tokens,
                    NULL,
                    :model_name,
                    :output_price,
                    false,
                    CURRENT_DATE,
                    :family_id,
                    CAST(:input_modalities AS jsonb),
                    CAST(:output_modalities AS jsonb)
                WHERE NOT EXISTS (
                    SELECT 1 FROM ai_models WHERE api_id = :api_id
                )
            """

            db.execute(text(sql), {
                "api_id": api_id,
                "category": category,
                "context_window": context_window,
                "input_price": input_price,
                "max_output_tokens": max_output_tokens,
                "model_name": model_name,
                "output_price": output_price,
                "family_id": family_id,
                "input_modalities": input_modalities_json,
                "output_modalities": output_modalities_json,
            })

        insert_model_if_missing(
            api_id="openai-gpt-4o",
            model_name="ChatGPT",
            category="chat",
            context_window=128000,
            input_price=5.00,
            output_price=15.00,
            max_output_tokens=16000,
            family_id=openai_family_id,
            input_modalities_json='["text", "image"]',
            output_modalities_json='["text"]',
        )

        insert_model_if_missing(
            api_id="google-gemini",
            model_name="Gemini",
            category="chat",
            context_window=1000000,
            input_price=3.50,
            output_price=10.00,
            max_output_tokens=8000,
            family_id=gemini_family_id,
            input_modalities_json='["text", "image"]',
            output_modalities_json='["text"]',
        )

        insert_model_if_missing(
            api_id="anthropic-claude",
            model_name="Claude",
            category="chat",
            context_window=200000,
            input_price=3.00,
            output_price=15.00,
            max_output_tokens=8000,
            family_id=claude_family_id,
            input_modalities_json='["text"]',
            output_modalities_json='["text"]',
        )

        db.commit()
        print("✅ AI 모델 + family + vendor seed 완료")

        # 확인 출력
        rows = db.execute(text("""
            SELECT id, model_name, category, family_id, is_preview
            FROM ai_models
            ORDER BY id
        """)).fetchall()

        print("=== ai_models ===")
        for row in rows:
            print(row)

    except Exception as e:
        db.rollback()
        print("❌ 실패:", e)

    finally:
        db.close()


if __name__ == "__main__":
    seed_ai_models()