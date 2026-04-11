import unittest

from gateway.input_normalizer import (
    GatewayValidationError,
    normalize_agent_type,
    normalize_flow_id,
    normalize_flow_step,
    normalize_mcp_personal_token,
    normalize_queries,
    normalize_user_input_confirmed,
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

    def test_normalize_flow_step_raises_when_invalid(self):
        with self.assertRaisesRegex(
                GatewayValidationError,
                "step must be one of: START, COLLECTED."
        ):
            normalize_flow_step("FETCH_SKILL")

    def test_normalize_flow_id(self):
        self.assertEqual(normalize_flow_id(" flow_1 "), "flow_1")

        with self.assertRaises(GatewayValidationError):
            normalize_flow_id(None)

        with self.assertRaises(GatewayValidationError):
            normalize_flow_id("   ")

    def test_normalize_user_input_confirmed_requires_true(self):
        self.assertTrue(normalize_user_input_confirmed(True))

    def test_normalize_user_input_confirmed_raises_when_false_or_none(self):
        with self.assertRaises(GatewayValidationError):
            normalize_user_input_confirmed(False)

        with self.assertRaises(GatewayValidationError):
            normalize_user_input_confirmed(None)


if __name__ == "__main__":
    unittest.main()
