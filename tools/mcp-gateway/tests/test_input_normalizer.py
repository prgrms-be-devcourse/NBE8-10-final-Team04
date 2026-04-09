import unittest

from gateway.input_normalizer import (
    GatewayValidationError,
    normalize_chunk_size,
    normalize_customization_applied,
    normalize_cursor,
    normalize_agent_type,
    normalize_finalize_decision,
    normalize_flow_step,
    normalize_flow_id,
    normalize_queries,
    normalize_mcp_personal_token,
    normalize_skill_id,
    normalize_user_decision_confirmed,
    normalize_user_input_confirmed,
    normalize_written_length,
    normalize_written_sha256,
)


class InputNormalizerTest(unittest.TestCase):
    def test_normalize_queries_collapses_whitespace(self):
        self.assertEqual(
            normalize_queries([" SpringBoot   ", " infra  DevOps "]),
            ["SpringBoot", "infra DevOps"],
        )

    def test_normalize_queries_raises_when_invalid(self):
        with self.assertRaises(GatewayValidationError):
            normalize_queries([])

        with self.assertRaises(GatewayValidationError):
            normalize_queries(["   "])

        with self.assertRaises(GatewayValidationError):
            normalize_queries("SpringBoot")  # type: ignore[arg-type]

    def test_normalize_agent_type_uppercases_and_validates(self):
        self.assertEqual(normalize_agent_type("claude"), "CLAUDE")

    def test_normalize_agent_type_raises_when_unsupported(self):
        with self.assertRaises(GatewayValidationError):
            normalize_agent_type("CHATGPT")

    def test_normalize_token_strips(self):
        self.assertEqual(normalize_mcp_personal_token("  mcp_abc  "), "mcp_abc")

    def test_normalize_token_raises_when_blank(self):
        with self.assertRaises(GatewayValidationError):
            normalize_mcp_personal_token("\n\t")

    def test_normalize_flow_step(self):
        self.assertEqual(normalize_flow_step("start"), "START")
        self.assertEqual(normalize_flow_step("COLLECTED"), "COLLECTED")
        self.assertEqual(normalize_flow_step("fetch_skill"), "FETCH_SKILL")
        self.assertEqual(normalize_flow_step("verify_skill"), "VERIFY_SKILL")
        self.assertEqual(normalize_flow_step("decide"), "DECIDE")
        self.assertEqual(normalize_flow_step(" finalize "), "FINALIZE")

    def test_normalize_flow_step_raises_when_invalid(self):
        with self.assertRaisesRegex(
                GatewayValidationError,
                "step must be one of: START, COLLECTED, FETCH_SKILL, VERIFY_SKILL, DECIDE, FINALIZE."
        ):
            normalize_flow_step("DONE")

    def test_normalize_flow_id(self):
        self.assertEqual(normalize_flow_id(" flow_1 "), "flow_1")

        with self.assertRaises(GatewayValidationError):
            normalize_flow_id(None)

        with self.assertRaises(GatewayValidationError):
            normalize_flow_id("   ")

    def test_normalize_finalize_decision(self):
        self.assertEqual(normalize_finalize_decision("accept"), "ACCEPT")
        self.assertEqual(normalize_finalize_decision(" CUSTOMIZE "), "CUSTOMIZE")

    def test_normalize_finalize_decision_raises_when_invalid(self):
        with self.assertRaises(GatewayValidationError):
            normalize_finalize_decision("SKIP")

    def test_normalize_user_input_confirmed_requires_true(self):
        self.assertTrue(normalize_user_input_confirmed(True))

    def test_normalize_user_input_confirmed_raises_when_false_or_none(self):
        with self.assertRaises(GatewayValidationError):
            normalize_user_input_confirmed(False)

        with self.assertRaises(GatewayValidationError):
            normalize_user_input_confirmed(None)

    def test_normalize_user_decision_confirmed_requires_true(self):
        self.assertTrue(normalize_user_decision_confirmed(True))

        with self.assertRaises(GatewayValidationError):
            normalize_user_decision_confirmed(False)

        with self.assertRaises(GatewayValidationError):
            normalize_user_decision_confirmed(None)

    def test_normalize_skill_id_requires_positive(self):
        self.assertEqual(normalize_skill_id(7), 7)
        self.assertEqual(normalize_skill_id("7"), 7)
        self.assertEqual(normalize_skill_id(7.0), 7)

        with self.assertRaises(GatewayValidationError):
            normalize_skill_id(0)

        with self.assertRaises(GatewayValidationError):
            normalize_skill_id(None)

        with self.assertRaises(GatewayValidationError):
            normalize_skill_id("bad")

        with self.assertRaises(GatewayValidationError):
            normalize_skill_id(3.14)

    def test_normalize_cursor(self):
        self.assertEqual(normalize_cursor(None), 0)
        self.assertEqual(normalize_cursor(3), 3)
        self.assertEqual(normalize_cursor("3"), 3)
        self.assertEqual(normalize_cursor(3.0), 3)

        with self.assertRaises(GatewayValidationError):
            normalize_cursor(-1)

        with self.assertRaises(GatewayValidationError):
            normalize_cursor("bad")

        with self.assertRaises(GatewayValidationError):
            normalize_cursor(2.7)

    def test_normalize_chunk_size(self):
        self.assertEqual(normalize_chunk_size(None), 3000)
        self.assertEqual(normalize_chunk_size(4096), 4096)
        self.assertEqual(normalize_chunk_size("4096"), 4096)
        self.assertEqual(normalize_chunk_size(4096.0), 4096)

        with self.assertRaises(GatewayValidationError):
            normalize_chunk_size(0)

        with self.assertRaises(GatewayValidationError):
            normalize_chunk_size(20001)

        with self.assertRaises(GatewayValidationError):
            normalize_chunk_size("bad")

        with self.assertRaises(GatewayValidationError):
            normalize_chunk_size(1024.5)

    def test_normalize_written_length(self):
        self.assertEqual(normalize_written_length(10), 10)
        self.assertEqual(normalize_written_length("10"), 10)

        with self.assertRaises(GatewayValidationError):
            normalize_written_length(-1)

        with self.assertRaises(GatewayValidationError):
            normalize_written_length(None)

    def test_normalize_written_sha256(self):
        good = "a" * 64
        self.assertEqual(normalize_written_sha256(good), good)
        self.assertEqual(normalize_written_sha256(f" {good} "), good)

        with self.assertRaises(GatewayValidationError):
            normalize_written_sha256(None)

        with self.assertRaises(GatewayValidationError):
            normalize_written_sha256("abc")

    def test_normalize_customization_applied(self):
        self.assertTrue(normalize_customization_applied(True))
        self.assertFalse(normalize_customization_applied(False))
        self.assertFalse(normalize_customization_applied(None))


if __name__ == "__main__":
    unittest.main()
