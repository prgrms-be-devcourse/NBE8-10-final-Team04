import unittest

from gateway.input_normalizer import (
    GatewayValidationError,
    normalize_agent_type,
    normalize_finalize_decision,
    normalize_flow_step,
    normalize_keywords,
    normalize_mcp_personal_token,
    normalize_user_input_confirmed,
)


class InputNormalizerTest(unittest.TestCase):
    def test_normalize_keywords_collapses_whitespace(self):
        self.assertEqual(normalize_keywords(" SpringBoot   infra  DevOps "), "SpringBoot infra DevOps")

    def test_normalize_keywords_raises_when_blank(self):
        with self.assertRaises(GatewayValidationError):
            normalize_keywords("   ")

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
        self.assertEqual(normalize_flow_step(" finalize "), "FINALIZE")

    def test_normalize_flow_step_raises_when_invalid(self):
        with self.assertRaises(GatewayValidationError):
            normalize_flow_step("DONE")

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


if __name__ == "__main__":
    unittest.main()
