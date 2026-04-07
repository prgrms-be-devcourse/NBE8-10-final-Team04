import unittest

from gateway.auto_flow_service import AutoFlowService
from gateway.input_normalizer import GatewayValidationError


class _StubSpringProxyClient:
    def __init__(self):
        self.last_template_request = None
        self.last_recommend_request = None
        self.last_skill_content_requests = []

    def get_start_agent_template(self, mcp_personal_token, agent_type):
        self.last_template_request = {
            "mcp_personal_token": mcp_personal_token,
            "agent_type": agent_type,
        }
        return {
            "data": {
                "templateName": "start.agent.md",
                "version": "v3",
                "templateMarkdown": "# START AGENT TEMPLATE (CODEX)\n",
            }
        }

    def recommend_skills(self, mcp_personal_token, keywords):
        self.last_recommend_request = {
            "mcp_personal_token": mcp_personal_token,
            "keywords": keywords,
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

    def get_recommendation_skill_content(self, mcp_personal_token, skill_id):
        self.last_skill_content_requests.append({
            "mcp_personal_token": mcp_personal_token,
            "skill_id": skill_id,
        })
        if skill_id == 1:
            return {
                "data": {
                    "skillId": 1,
                    "category": "backend",
                    "sourceRepo": "example/doc-agent",
                    "skillMdRaw": "Spring Backend Code Review",
                }
            }

        return {
            "data": {
                "skillId": 3,
                "category": "infra",
                "sourceRepo": "example/oci-infra-kit",
                "skillMdRaw": "OCI Infrastructure Bootstrap",
            }
        }


class _StubClientWithEmptyRecommendation:
    def get_start_agent_template(self, mcp_personal_token, agent_type):
        return {
            "data": {
                "templateName": "start.agent.md",
                "version": "v3",
                "templateMarkdown": "# template",
            }
        }

    def recommend_skills(self, mcp_personal_token, keywords):
        return {"data": {"selectedSkills": []}}

    def get_recommendation_skill_content(self, mcp_personal_token, skill_id):
        return {"data": {"skillId": skill_id, "category": "unknown", "sourceRepo": "unknown", "skillMdRaw": "# x"}}


class _StubClientWithStringSkillId:
    def get_start_agent_template(self, mcp_personal_token, agent_type):
        return {
            "data": {
                "templateName": "start.agent.md",
                "version": "v3",
                "templateMarkdown": "# template",
            }
        }

    def recommend_skills(self, mcp_personal_token, keywords):
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

    def get_recommendation_skill_content(self, mcp_personal_token, skill_id):
        return {
            "data": {
                "skillId": skill_id,
                "category": "backend",
                "sourceRepo": "example/doc-agent",
                "skillMdRaw": "Spring Backend Code Review",
            }
        }


class _StubClientWithMissingSkillContentMeta:
    def get_start_agent_template(self, mcp_personal_token, agent_type):
        return {
            "data": {
                "templateName": "start.agent.md",
                "version": "v3",
                "templateMarkdown": "# template",
            }
        }

    def recommend_skills(self, mcp_personal_token, keywords):
        return {
            "data": {
                "selectedSkills": [
                    {
                        "category": "backend",
                        "skillId": 11,
                        "finalScore": 0.77,
                        "sourceRepo": "example/doc-agent",
                    }
                ]
            }
        }

    def get_recommendation_skill_content(self, mcp_personal_token, skill_id):
        return {
            "data": {
                "skillId": skill_id,
                "skillMdRaw": "Spring Backend Code Review",
            }
        }


class AutoFlowServiceTest(unittest.TestCase):
    def setUp(self):
        self.stub_client = _StubSpringProxyClient()
        self.service = AutoFlowService(self.stub_client)

    def test_start_step_returns_start_agent_write_action(self):
        response = self.service.run(
            step="START",
            mcp_personal_token=None,
            agent_type="codex",
            keywords=None,
            user_input_confirmed=None,
            decision=None,
            customization_notes=None,
        )

        self.assertTrue(response["success"])
        self.assertEqual(response["flowStep"], "START")
        self.assertEqual(response["actions"]["writeFiles"][0]["path"], "start.agent.md")
        self.assertEqual(response["actions"]["nextStep"], "COLLECTED")
        self.assertEqual(self.stub_client.last_template_request["agent_type"], "CODEX")

    def test_start_step_uses_default_agent_type_when_missing(self):
        response = self.service.run(
            step="START",
            mcp_personal_token=None,
            agent_type=None,
            keywords=None,
            user_input_confirmed=None,
            decision=None,
            customization_notes=None,
        )

        self.assertTrue(response["success"])
        self.assertEqual(self.stub_client.last_template_request["agent_type"], "CODEX")

    def test_collected_step_returns_skill_file_actions(self):
        response = self.service.run(
            step="COLLECTED",
            mcp_personal_token="mcp_token_1",
            agent_type=None,
            keywords=" SpringBoot   infra ",
            user_input_confirmed=True,
            decision=None,
            customization_notes=None,
        )

        self.assertTrue(response["success"])
        self.assertEqual(response["flowStep"], "COLLECTED")
        self.assertEqual(response["actions"]["nextStep"], "FINALIZE")
        self.assertEqual(len(response["actions"]["writeFiles"]), 2)
        self.assertEqual(response["actions"]["writeFiles"][0]["path"], "skills/backend.md")
        self.assertIn("기존 skills 파일을 기반으로", response["actions"]["askUser"][2])
        self.assertIn("처음부터 새로 작성하지 말고", response["actions"]["askUser"][3])
        self.assertEqual(self.stub_client.last_recommend_request["keywords"], "SpringBoot infra")
        self.assertEqual([1, 3], [req["skill_id"] for req in self.stub_client.last_skill_content_requests])
        self.assertIsInstance(response["recommendation"]["selectedSkills"][0]["skillId"], int)
        self.assertEqual(response["recommendation"]["selectedSkills"][0]["skillId"], 1)

    def test_finalize_step_returns_agents_write_and_start_file_delete_actions(self):
        response = self.service.run(
            step="FINALIZE",
            mcp_personal_token="mcp_token_1",
            agent_type=None,
            keywords="SpringBoot infra",
            user_input_confirmed=None,
            decision="customize",
            customization_notes="OCI 비용 제약 반영",
        )

        self.assertTrue(response["success"])
        self.assertEqual(response["flowStep"], "FINALIZE")
        self.assertEqual(response["finalize"]["decision"], "CUSTOMIZE")
        self.assertIn("기존 skills 파일을 기반으로", response["finalize"]["customizationPolicy"])
        self.assertIn("start.agent.md", response["actions"]["deleteFiles"])

        write_paths = [item["path"] for item in response["actions"]["writeFiles"]]
        self.assertIn("agents.md", write_paths)
        self.assertNotIn("skills/backend.md", write_paths)
        self.assertNotIn("skills/infra.md", write_paths)

        agents_file = next(
            item for item in response["actions"]["writeFiles"]
            if item["path"] == "agents.md"
        )
        self.assertIn("Customize Rule", agents_file["content"])
        self.assertIn("새 문서를 처음부터 다시 작성하지 않는다", agents_file["content"])
        self.assertIsNone(self.stub_client.last_recommend_request)

    def test_finalize_step_accept_writes_skill_files(self):
        response = self.service.run(
            step="FINALIZE",
            mcp_personal_token="mcp_token_1",
            agent_type=None,
            keywords="SpringBoot infra",
            user_input_confirmed=None,
            decision="accept",
            customization_notes=None,
        )

        write_paths = [item["path"] for item in response["actions"]["writeFiles"]]
        self.assertIn("agents.md", write_paths)
        self.assertNotIn("skills/backend.md", write_paths)
        self.assertNotIn("skills/infra.md", write_paths)

    def test_finalize_step_requires_decision(self):
        with self.assertRaises(GatewayValidationError):
            self.service.run(
                step="FINALIZE",
                mcp_personal_token="mcp_token_1",
                agent_type=None,
                keywords="SpringBoot infra",
                user_input_confirmed=None,
                decision=None,
                customization_notes=None,
            )

    def test_collected_step_raises_when_selected_skills_missing_or_empty(self):
        service = AutoFlowService(_StubClientWithEmptyRecommendation())

        with self.assertRaises(GatewayValidationError):
            service.run(
                step="COLLECTED",
                mcp_personal_token="mcp_token_1",
                agent_type=None,
                keywords="SpringBoot infra",
                user_input_confirmed=True,
                decision=None,
                customization_notes=None,
            )

    def test_collected_step_parses_string_skill_id_to_int(self):
        service = AutoFlowService(_StubClientWithStringSkillId())

        response = service.run(
            step="COLLECTED",
            mcp_personal_token="mcp_token_1",
            agent_type=None,
            keywords="SpringBoot infra",
            user_input_confirmed=True,
            decision=None,
            customization_notes=None,
        )

        self.assertEqual(response["recommendation"]["selectedSkills"][0]["skillId"], 7)
        self.assertIsInstance(response["recommendation"]["selectedSkills"][0]["skillId"], int)

    def test_collected_step_falls_back_to_summary_meta_when_content_meta_missing(self):
        service = AutoFlowService(_StubClientWithMissingSkillContentMeta())

        response = service.run(
            step="COLLECTED",
            mcp_personal_token="mcp_token_1",
            agent_type=None,
            keywords="SpringBoot infra",
            user_input_confirmed=True,
            decision=None,
            customization_notes=None,
        )

        selected_skill = response["recommendation"]["selectedSkills"][0]
        self.assertEqual(selected_skill["category"], "backend")
        self.assertEqual(selected_skill["sourceRepo"], "example/doc-agent")

    def test_collected_step_requires_user_input_confirmed_true(self):
        with self.assertRaises(GatewayValidationError):
            self.service.run(
                step="COLLECTED",
                mcp_personal_token="mcp_token_1",
                agent_type=None,
                keywords="SpringBoot infra",
                user_input_confirmed=False,
                decision=None,
                customization_notes=None,
            )

        with self.assertRaises(GatewayValidationError):
            self.service.run(
                step="COLLECTED",
                mcp_personal_token="mcp_token_1",
                agent_type=None,
                keywords="SpringBoot infra",
                user_input_confirmed=None,
                decision=None,
                customization_notes=None,
            )


if __name__ == "__main__":
    unittest.main()
