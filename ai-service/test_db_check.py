from sqlalchemy import text
from app.db.database import SessionLocal


def check_db():
    db = SessionLocal()

    try:
        print("=" * 80)
        print("1. DB 연결 확인")

        result = db.execute(text("SELECT 1 AS connected;"))
        row = result.fetchone()
        print("DB 연결 성공:", row.connected)

        print("=" * 80)
        print("2. public 테이블 목록 조회")

        tables = db.execute(text("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
            ORDER BY table_name;
        """)).fetchall()

        if not tables:
            print("테이블이 하나도 없습니다.")
        else:
            for table in tables:
                print("-", table.table_name)

        print("=" * 80)
        print("3. 주요 테이블 컬럼 조회")

        target_tables = [
            "ai_models",
            "skills",
            "skill_chunks",
            "agents",
            "repositories",
        ]

        for table_name in target_tables:
            print(f"\n[테이블] {table_name}")

            columns = db.execute(text("""
                SELECT column_name, data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = :table_name
                ORDER BY ordinal_position;
            """), {"table_name": table_name}).fetchall()

            if not columns:
                print("  -> 테이블 없거나 컬럼 조회 실패")
                continue

            for col in columns:
                print(f"  - {col.column_name} ({col.data_type})")

        print("=" * 80)
        print("4. 데이터 개수 확인")

        count_tables = [
            "ai_models",
            "skills",
            "skill_chunks",
        ]

        for table_name in count_tables:
            try:
                count_result = db.execute(text(f"SELECT COUNT(*) AS cnt FROM {table_name};"))
                count_row = count_result.fetchone()
                print(f"{table_name}: {count_row.cnt} rows")
            except Exception as e:
                print(f"{table_name}: 조회 실패 -> {e}")

    except Exception as e:
        print("DB 검사 실패:", e)

    finally:
        db.close()


if __name__ == "__main__":
    check_db()