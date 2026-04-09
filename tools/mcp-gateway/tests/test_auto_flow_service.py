import unittest

from gateway.auto_flow_service import AutoFlowService
from gateway.input_normalizer import GatewayValidationError


class _StubSpringProxyClient:
    def __init__(self):
        self.last_template_request = None
        self.last_recommend_request = None

    def get_start_agent_template(self, mcp_personal_token, agent_type):
        self.last_template_request = {
            "mcp_personal_token": mcp_personal_token,
            "agent_type": agent_type,
        }
        return {
            "data": {
                "templateName": "start.agent.md",
                "version": "v6",
                "templateMarkdown": "# START AGENT TEMPLATE (CODEX)\n",
            }
        }

    def recommend_skills(self, mcp_personal_token, queries):
        self.last_recommend_request = {
            "mcp_personal_token": mcp_personal_token,
            "queries": queries,
        }
        return {
            "data": {
                "selectedSkills": [
                    {
                        "category": "backend",
                        "skillId": 1,
                        "finalScore": 0.91,
                        "sourceRepo": "example/doc-agent",
                    },
                    {
                        "category": "infra",
                        "skillId": 3,
                        "finalScore": 0.82,
                        "sourceRepo": "example/oci-infra-kit",
                    },
                ]
            }
        }


class _StubClientWithEmptyRecommendation:
    def get_start_agent_template(self, mcp_personal_token, agent_type):
        return {
            "data": {
                "templateName": "start.agent.md",
                "version": "v6",
                "templateMarkdown": "# template",
            }
        }

    def recommend_skills(self, mcp_personal_token, queries):
        return {"data": {"selectedSkills": []}}


class _StubClientWithStringSkillId:
    def get_start_agent_template(self, mcp_personal_token, agent_type):
        return {
            "data": {
                "templateName": "start.agent.md",
                "version": "v6",
                "templateMarkdown": "# template",
            }
        }

    def recommend_skills(self, mcp_personal_token, queries):
        return {
            "data": {
                "selectedSkills": [
                    {
                        "category": "backend",
                        "skillId": "7",
                        "finalScore": 0.91,
                        "sourceRepo": "example/doc-agent",
                    }
                ]
            }
        }


class AutoFlowServiceTest(unittest.TestCase):
    def setUp(self):
        self.stub_client = _StubSpringProxyClient()
        self.service = AutoFlowService(self.stub_client)
        self.mcp_token = "mcp_token_1"

    def _start_flow(self) -> str:
        response = self.service.run(
            step="START",
            mcp_personal_token=self.mcp_token,
            agent_type="codex",
            flow_id=None,
            queries=None,
            user_input_confirmed=None,
        )
        return response["flowId"]

    def _collect(self, flow_id: str, queries: list[str] | None = None) -> dict:
        effective_queries = queries if queries is not None else ["SpringBoot", "infra"]
        return self.service.run(
            step="COLLECTED",
            mcp_personal_token=self.mcp_token,
            agent_type=None,
            flow_id=flow_id,
            queries=effective_queries,
            user_input_confirmed=True,
        )

    def test_start_step_returns_flow_id_and_start_actions(self):
        response = self.service.run(
            step="START",
            mcp_personal_token=self.mcp_token,
            agent_type="codex",
            flow_id=None,
            queries=None,
            user_input_confirmed=None,
        )

        self.assertTrue(response["success"])
        self.assertEqual(response["flowStep"], "START")
        self.assertEqual(response["actions"]["writeFiles"][0]["path"], "start.agent.md")
        self.assertEqual(response["actions"]["nextStep"], "COLLECTED")
        self.assertIsInstance(response["flowId"], str)
        self.assertEqual(self.stub_client.last_template_request["agent_type"], "CODEX")

    def test_collected_step_requires_flow_id(self):
        with self.assertRaises(GatewayValidationError):
            self.service.run(
                step="COLLECTED",
                mcp_personal_token=self.mcp_token,
                agent_type=None,
                flow_id=None,
                queries=["SpringBoot", "infra"],
                user_input_confirmed=True,
            )

    def test_collected_step_returns_runner_plan(self):
        flow_id = self._start_flow()
        response = self._collect(flow_id, queries=[" SpringBoot   ", "infra "])

        self.assertEqual(response["flowStep"], "COLLECTED")
        self.assertEqual(response["actions"]["nextStep"], "DONE")
        self.assertEqual(response["runner"]["entrypoint"], "tools/mcp-gateway/runner/generate_skills.py")
        self.assertEqual(self.stub_client.last_recommend_request["queries"], ["SpringBoot", "infra"])
        self.assertIsInstance(response["recommendation"]["selectedSkills"][0]["skillId"], int)
        self.assertEqual(response["recommendation"]["selectedSkills"][0]["skillId"], 1)
        self.assertIn("skills/backend.md", response["runner"]["expectedFiles"])
        self.assertIn("agents.md", response["runner"]["expectedFiles"])

    def test_collected_step_fails_when_called_twice(self):
        flow_id = self._start_flow()
        self._collect(flow_id)

        with self.assertRaises(GatewayValidationError):
            self._collect(flow_id)

    def test_collected_step_raises_when_selected_skills_missing_or_empty(self):
        service = AutoFlowService(_StubClientWithEmptyRecommendation())
        start_response = service.run(
            step="START",
            mcp_personal_token=self.mcp_token,
            agent_type="codex",
            flow_id=None,
            queries=None,
            user_input_confirmed=None,
        )

        with self.assertRaises(GatewayValidationError):
            service.run(
                step="COLLECTED",
                mcp_personal_token=self.mcp_token,
                agent_type=None,
                flow_id=start_response["flowId"],
                queries=["SpringBoot", "infra"],
                user_input_confirmed=True,
            )

    def test_collected_step_parses_string_skill_id_to_int(self):
        service = AutoFlowService(_StubClientWithStringSkillId())
        start_response = service.run(
            step="START",
            mcp_personal_token=self.mcp_token,
            agent_type="codex",
            flow_id=None,
            queries=None,
            user_input_confirmed=None,
        )

        response = service.run(
            step="COLLECTED",
            mcp_personal_token=self.mcp_token,
            agent_type=None,
            flow_id=start_response["flowId"],
            queries=["SpringBoot", "infra"],
            user_input_confirmed=True,
        )

        self.assertEqual(response["recommendation"]["selectedSkills"][0]["skillId"], 7)
        self.assertIsInstance(response["recommendation"]["selectedSkills"][0]["skillId"], int)

    def test_rejects_unsupported_step_in_direct_runner_mode(self):
        flow_id = self._start_flow()
        with self.assertRaisesRegex(GatewayValidationError, "step must be one of: START, COLLECTED"):
            self.service.run(
                step="FETCH_SKILL",
                mcp_personal_token=self.mcp_token,
                agent_type=None,
                flow_id=flow_id,
                queries=None,
                user_input_confirmed=None,
            )


if __name__ == "__main__":
    unittest.main()
