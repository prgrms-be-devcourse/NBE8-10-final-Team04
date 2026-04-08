import hashlib
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
                "version": "v4",
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
                "version": "v4",
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
                "version": "v4",
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
            keywords=None,
            user_input_confirmed=None,
            skill_id=None,
            cursor=None,
            chunk_size=None,
            written_length=None,
            written_sha256=None,
            user_decision_confirmed=None,
            decision=None,
            customization_notes=None,
            customization_applied=None,
        )
        return response["flowId"]

    def _collect(self, flow_id: str, keywords: str = "SpringBoot infra") -> dict:
        return self.service.run(
            step="COLLECTED",
            mcp_personal_token=self.mcp_token,
            agent_type=None,
            flow_id=flow_id,
            keywords=keywords,
            user_input_confirmed=True,
            skill_id=None,
            cursor=None,
            chunk_size=None,
            written_length=None,
            written_sha256=None,
            user_decision_confirmed=None,
            decision=None,
            customization_notes=None,
            customization_applied=None,
        )

    def _fetch_until_done(self, flow_id: str, skill_id: int, chunk_size: int = 3000) -> dict:
        cursor = 0
        latest = None
        while True:
            latest = self.service.run(
                step="FETCH_SKILL",
                mcp_personal_token=self.mcp_token,
                agent_type=None,
                flow_id=flow_id,
                keywords=None,
                user_input_confirmed=None,
                skill_id=skill_id,
                cursor=cursor,
                chunk_size=chunk_size,
                written_length=None,
                written_sha256=None,
                user_decision_confirmed=None,
                decision=None,
                customization_notes=None,
                customization_applied=None,
            )
            if not latest["skillChunk"]["hasNext"]:
                return latest
            cursor = latest["skillChunk"]["nextCursor"]

    def _verify(self, flow_id: str, skill_id: int, expected_file_content: str) -> dict:
        digest = hashlib.sha256(expected_file_content.encode("utf-8")).hexdigest()
        return self.service.run(
            step="VERIFY_SKILL",
            mcp_personal_token=self.mcp_token,
            agent_type=None,
            flow_id=flow_id,
            keywords=None,
            user_input_confirmed=None,
            skill_id=skill_id,
            cursor=None,
            chunk_size=None,
            written_length=len(expected_file_content),
            written_sha256=digest,
            user_decision_confirmed=None,
            decision=None,
            customization_notes=None,
            customization_applied=None,
        )

    def test_start_step_returns_flow_id_and_start_actions(self):
        response = self.service.run(
            step="START",
            mcp_personal_token=self.mcp_token,
            agent_type="codex",
            flow_id=None,
            keywords=None,
            user_input_confirmed=None,
            skill_id=None,
            cursor=None,
            chunk_size=None,
            written_length=None,
            written_sha256=None,
            user_decision_confirmed=None,
            decision=None,
            customization_notes=None,
            customization_applied=None,
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
                keywords="SpringBoot infra",
                user_input_confirmed=True,
                skill_id=None,
                cursor=None,
                chunk_size=None,
                written_length=None,
                written_sha256=None,
                user_decision_confirmed=None,
                decision=None,
                customization_notes=None,
                customization_applied=None,
            )

    def test_collected_step_returns_selected_skill_metadata(self):
        flow_id = self._start_flow()
        response = self._collect(flow_id, keywords=" SpringBoot   infra ")

        self.assertEqual(response["flowStep"], "COLLECTED")
        self.assertEqual(response["actions"]["nextStep"], "FETCH_SKILL")
        self.assertEqual(response["actions"]["nextStepParamsExample"]["flowId"], flow_id)
        self.assertEqual(self.stub_client.last_recommend_request["keywords"], "SpringBoot infra")
        self.assertIsInstance(response["recommendation"]["selectedSkills"][0]["skillId"], int)
        self.assertEqual(response["recommendation"]["selectedSkills"][0]["skillId"], 1)

    def test_fetch_skill_returns_integrity_metadata(self):
        flow_id = self._start_flow()
        self._collect(flow_id)
        response = self._fetch_until_done(flow_id, skill_id=1, chunk_size=6)

        self.assertEqual(response["flowStep"], "FETCH_SKILL")
        self.assertEqual(response["skillChunk"]["skillId"], 1)
        self.assertIn("expectedFileLength", response["integrity"])
        self.assertIn("expectedFileSha256", response["integrity"])

    def test_verify_skill_rejects_mismatched_hash(self):
        flow_id = self._start_flow()
        self._collect(flow_id)
        self._fetch_until_done(flow_id, skill_id=1)

        with self.assertRaises(GatewayValidationError):
            self.service.run(
                step="VERIFY_SKILL",
                mcp_personal_token=self.mcp_token,
                agent_type=None,
                flow_id=flow_id,
                keywords=None,
                user_input_confirmed=None,
                skill_id=1,
                cursor=None,
                chunk_size=None,
                written_length=1,
                written_sha256="a" * 64,
                user_decision_confirmed=None,
                decision=None,
                customization_notes=None,
                customization_applied=None,
            )

    def test_finalize_requires_decide_step(self):
        flow_id = self._start_flow()
        self._collect(flow_id)
        self._fetch_until_done(flow_id, skill_id=1)
        self._fetch_until_done(flow_id, skill_id=3)

        expected_backend = self.service._build_skill_markdown_from_content(
            skill_id=1,
            category="backend",
            source_repo="example/doc-agent",
            raw_content="Spring Backend Code Review",
        )
        expected_infra = self.service._build_skill_markdown_from_content(
            skill_id=3,
            category="infra",
            source_repo="example/oci-infra-kit",
            raw_content="OCI Infrastructure Bootstrap",
        )

        self._verify(flow_id, 1, expected_backend)
        self._verify(flow_id, 3, expected_infra)

        with self.assertRaises(GatewayValidationError):
            self.service.run(
                step="FINALIZE",
                mcp_personal_token=self.mcp_token,
                agent_type=None,
                flow_id=flow_id,
                keywords=None,
                user_input_confirmed=None,
                skill_id=None,
                cursor=None,
                chunk_size=None,
                written_length=None,
                written_sha256=None,
                user_decision_confirmed=None,
                decision=None,
                customization_notes=None,
                customization_applied=None,
            )

    def test_decide_requires_user_decision_confirmed(self):
        flow_id = self._start_flow()
        self._collect(flow_id)
        self._fetch_until_done(flow_id, skill_id=1)
        self._fetch_until_done(flow_id, skill_id=3)

        expected_backend = self.service._build_skill_markdown_from_content(
            skill_id=1,
            category="backend",
            source_repo="example/doc-agent",
            raw_content="Spring Backend Code Review",
        )
        expected_infra = self.service._build_skill_markdown_from_content(
            skill_id=3,
            category="infra",
            source_repo="example/oci-infra-kit",
            raw_content="OCI Infrastructure Bootstrap",
        )
        self._verify(flow_id, 1, expected_backend)
        self._verify(flow_id, 3, expected_infra)

        with self.assertRaises(GatewayValidationError):
            self.service.run(
                step="DECIDE",
                mcp_personal_token=self.mcp_token,
                agent_type=None,
                flow_id=flow_id,
                keywords=None,
                user_input_confirmed=None,
                skill_id=None,
                cursor=None,
                chunk_size=None,
                written_length=None,
                written_sha256=None,
                user_decision_confirmed=False,
                decision="ACCEPT",
                customization_notes=None,
                customization_applied=None,
            )

    def test_customize_finalize_requires_customization_applied_true(self):
        flow_id = self._start_flow()
        self._collect(flow_id)
        self._fetch_until_done(flow_id, skill_id=1)
        self._fetch_until_done(flow_id, skill_id=3)

        expected_backend = self.service._build_skill_markdown_from_content(
            skill_id=1,
            category="backend",
            source_repo="example/doc-agent",
            raw_content="Spring Backend Code Review",
        )
        expected_infra = self.service._build_skill_markdown_from_content(
            skill_id=3,
            category="infra",
            source_repo="example/oci-infra-kit",
            raw_content="OCI Infrastructure Bootstrap",
        )
        self._verify(flow_id, 1, expected_backend)
        self._verify(flow_id, 3, expected_infra)

        self.service.run(
            step="DECIDE",
            mcp_personal_token=self.mcp_token,
            agent_type=None,
            flow_id=flow_id,
            keywords=None,
            user_input_confirmed=None,
            skill_id=None,
            cursor=None,
            chunk_size=None,
            written_length=None,
            written_sha256=None,
            user_decision_confirmed=True,
            decision="CUSTOMIZE",
            customization_notes="사용자 요구 반영",
            customization_applied=None,
        )

        with self.assertRaises(GatewayValidationError):
            self.service.run(
                step="FINALIZE",
                mcp_personal_token=self.mcp_token,
                agent_type=None,
                flow_id=flow_id,
                keywords=None,
                user_input_confirmed=None,
                skill_id=None,
                cursor=None,
                chunk_size=None,
                written_length=None,
                written_sha256=None,
                user_decision_confirmed=None,
                decision=None,
                customization_notes=None,
                customization_applied=False,
            )

    def test_happy_path_accept_finalize(self):
        flow_id = self._start_flow()
        self._collect(flow_id)
        self._fetch_until_done(flow_id, skill_id=1)
        self._fetch_until_done(flow_id, skill_id=3)

        expected_backend = self.service._build_skill_markdown_from_content(
            skill_id=1,
            category="backend",
            source_repo="example/doc-agent",
            raw_content="Spring Backend Code Review",
        )
        expected_infra = self.service._build_skill_markdown_from_content(
            skill_id=3,
            category="infra",
            source_repo="example/oci-infra-kit",
            raw_content="OCI Infrastructure Bootstrap",
        )
        verify_backend = self._verify(flow_id, 1, expected_backend)
        verify_infra = self._verify(flow_id, 3, expected_infra)
        self.assertEqual(verify_backend["actions"]["nextStep"], "VERIFY_SKILL")
        self.assertEqual(verify_infra["actions"]["nextStep"], "DECIDE")

        decide_response = self.service.run(
            step="DECIDE",
            mcp_personal_token=self.mcp_token,
            agent_type=None,
            flow_id=flow_id,
            keywords=None,
            user_input_confirmed=None,
            skill_id=None,
            cursor=None,
            chunk_size=None,
            written_length=None,
            written_sha256=None,
            user_decision_confirmed=True,
            decision="ACCEPT",
            customization_notes=None,
            customization_applied=None,
        )
        self.assertEqual(decide_response["actions"]["nextStep"], "FINALIZE")

        finalize_response = self.service.run(
            step="FINALIZE",
            mcp_personal_token=self.mcp_token,
            agent_type=None,
            flow_id=flow_id,
            keywords=None,
            user_input_confirmed=None,
            skill_id=None,
            cursor=None,
            chunk_size=None,
            written_length=None,
            written_sha256=None,
            user_decision_confirmed=None,
            decision=None,
            customization_notes=None,
            customization_applied=False,
        )
        self.assertTrue(finalize_response["success"])
        self.assertEqual(finalize_response["flowStep"], "FINALIZE")
        self.assertEqual(finalize_response["finalize"]["decision"], "ACCEPT")

    def test_collected_step_raises_when_selected_skills_missing_or_empty(self):
        service = AutoFlowService(_StubClientWithEmptyRecommendation())
        start_response = service.run(
            step="START",
            mcp_personal_token=self.mcp_token,
            agent_type="codex",
            flow_id=None,
            keywords=None,
            user_input_confirmed=None,
            skill_id=None,
            cursor=None,
            chunk_size=None,
            written_length=None,
            written_sha256=None,
            user_decision_confirmed=None,
            decision=None,
            customization_notes=None,
            customization_applied=None,
        )

        with self.assertRaises(GatewayValidationError):
            service.run(
                step="COLLECTED",
                mcp_personal_token=self.mcp_token,
                agent_type=None,
                flow_id=start_response["flowId"],
                keywords="SpringBoot infra",
                user_input_confirmed=True,
                skill_id=None,
                cursor=None,
                chunk_size=None,
                written_length=None,
                written_sha256=None,
                user_decision_confirmed=None,
                decision=None,
                customization_notes=None,
                customization_applied=None,
            )

    def test_collected_step_parses_string_skill_id_to_int(self):
        service = AutoFlowService(_StubClientWithStringSkillId())
        start_response = service.run(
            step="START",
            mcp_personal_token=self.mcp_token,
            agent_type="codex",
            flow_id=None,
            keywords=None,
            user_input_confirmed=None,
            skill_id=None,
            cursor=None,
            chunk_size=None,
            written_length=None,
            written_sha256=None,
            user_decision_confirmed=None,
            decision=None,
            customization_notes=None,
            customization_applied=None,
        )

        response = service.run(
            step="COLLECTED",
            mcp_personal_token=self.mcp_token,
            agent_type=None,
            flow_id=start_response["flowId"],
            keywords="SpringBoot infra",
            user_input_confirmed=True,
            skill_id=None,
            cursor=None,
            chunk_size=None,
            written_length=None,
            written_sha256=None,
            user_decision_confirmed=None,
            decision=None,
            customization_notes=None,
            customization_applied=None,
        )

        self.assertEqual(response["recommendation"]["selectedSkills"][0]["skillId"], 7)
        self.assertIsInstance(response["recommendation"]["selectedSkills"][0]["skillId"], int)


if __name__ == "__main__":
    unittest.main()
