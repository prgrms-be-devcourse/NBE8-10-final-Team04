import unittest

from runner.generate_skills import (
    RunnerError,
    _build_skill_markdown,
    _normalize_queries,
    _parse_selected_skills,
    _resolve_skill_relative_path,
)


class GenerateSkillsRunnerTest(unittest.TestCase):
    def test_normalize_queries(self):
        self.assertEqual(
            _normalize_queries('[" SpringBoot ", " infra  DevOps "]'),
            ["SpringBoot", "infra DevOps"],
        )

        with self.assertRaises(RunnerError):
            _normalize_queries('"SpringBoot"')

        with self.assertRaises(RunnerError):
            _normalize_queries('["   "]')

    def test_parse_selected_skills_accepts_string_skill_id(self):
        response = {
            "data": {
                "selectedSkills": [
                    {
                        "skillId": "7",
                        "category": "backend",
                        "sourceRepo": "example/repo",
                        "finalScore": 0.9,
                    }
                ]
            }
        }

        skills = _parse_selected_skills(response)
        self.assertEqual(len(skills), 1)
        self.assertEqual(skills[0].skill_id, 7)
        self.assertEqual(skills[0].category, "backend")

    def test_resolve_skill_relative_path_avoids_duplicate_category_collision(self):
        used_paths: set[str] = set()

        first = _resolve_skill_relative_path(category="backend", skill_id=1, used_paths=used_paths)
        second = _resolve_skill_relative_path(category="backend", skill_id=2, used_paths=used_paths)

        self.assertEqual(first, "skills/backend.md")
        self.assertEqual(second, "skills/backend-2.md")

    def test_build_skill_markdown_preserves_raw_content(self):
        markdown = _build_skill_markdown(
            skill_id=11,
            category="backend",
            source_repo="example/repo",
            raw_content="  \n# raw\n",
        )

        self.assertTrue(markdown.endswith("  \n# raw\n"))


if __name__ == "__main__":
    unittest.main()
