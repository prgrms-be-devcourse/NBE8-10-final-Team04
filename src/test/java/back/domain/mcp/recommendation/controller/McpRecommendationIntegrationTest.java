package back.domain.mcp.recommendation.controller;

import back.domain.auth.entity.McpToken;
import back.domain.auth.repository.McpTokenRepository;
import back.domain.auth.util.McpTokenHasher;
import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.domain.payment.entity.Subscription;
import back.domain.payment.entity.SubscriptionPlanType;
import back.domain.payment.repository.SubscriptionRepository;
import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.repository.RepositoryRepository;
import back.domain.prompt.prompt.repository.SkillRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import back.global.security.AuthenticatedMember;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.mcp.recommendation.candidate-source=mock")
class McpRecommendationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private McpTokenRepository mcpTokenRepository;

    @Autowired
    private McpTokenHasher mcpTokenHasher;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    @DisplayName("유효한 MCP 토큰으로 추천 API를 호출하면 추천 결과를 반환한다")
    void recommend_success() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-901", "u901@example.com", "User 901"));

        subscriptionRepository.save(
                Subscription.builder()
                        .member(member)
                        .planType(SubscriptionPlanType.MONTHLY_990)
                        .amount(990)
                        .nextBillingAt(LocalDateTime.now().plusDays(30))
                        .build()
        );

        AuthenticatedMember authMember = new AuthenticatedMember(member.getId(), "ROLE_USER");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authMember,
                        null,
                        authMember.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String rawMcpToken = "mcp_recommend_token_901";

        McpToken mcpToken = McpToken.issue(
                member.getId(),
                mcpTokenHasher.hash(rawMcpToken),
                rawMcpToken.substring(0, Math.min(rawMcpToken.length(), 12)),
                "Codex",
                LocalDateTime.now().plusDays(3));
        mcpTokenRepository.save(mcpToken);

        mockMvc.perform(post("/api/v1/mcp/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(rawMcpToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keywords": "SpringBoot infra DevOps"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("추천 성공"))
                .andExpect(jsonPath("$.data.selectedSkills").isArray())
                .andExpect(jsonPath("$.data.selectedSkills[0].skillMdRaw").doesNotExist());
    }

    @Test
    @DisplayName("유효한 MCP 토큰으로 본문 조회 API를 호출하면 스킬 원문을 반환한다")
    void getSkillContent_success() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-903", "u903@example.com", "User 903"));
        String rawMcpToken = "mcp_recommend_token_903";

        McpToken mcpToken = McpToken.issue(
                member.getId(),
                mcpTokenHasher.hash(rawMcpToken),
                rawMcpToken.substring(0, Math.min(rawMcpToken.length(), 12)),
                "Codex",
                LocalDateTime.now().plusDays(3));
        mcpTokenRepository.save(mcpToken);

        Repository repository = repositoryRepository.save(Repository.builder()
                .githubId(9001L)
                .name("skill-repo")
                .sourceRepo("example/skill-repo")
                .sourceUri("https://github.com/example/skill-repo")
                .active(true)
                .build());

        Skill skill = skillRepository.save(Skill.builder()
                .repository(repository)
                .name("backend-skill")
                .contentMd("# backend skill content")
                .filePath("skills/backend.md")
                .category(Category.BACKEND)
                .build());

        mockMvc.perform(post("/api/v1/mcp/recommendations/skill-content")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(rawMcpToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\": %d}".formatted(skill.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("스킬 본문 조회 성공"))
                .andExpect(jsonPath("$.data.skillId").value(skill.getId()))
                .andExpect(jsonPath("$.data.category").value("backend"))
                .andExpect(jsonPath("$.data.sourceRepo").value("example/skill-repo"))
                .andExpect(jsonPath("$.data.skillMdRaw").value("# backend skill content"));
    }

    @Test
    @DisplayName("유효하지 않은 MCP 토큰이면 401을 반환한다")
    void recommend_whenInvalidToken() throws Exception {
        mockMvc.perform(post("/api/v1/mcp/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mcp_invalid_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keywords": "SpringBoot"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("MCP 토큰 인증 실패"));
    }

    @Test
    @DisplayName("keywords가 공백이면 400을 반환한다")
    void recommend_whenKeywordsIsBlank() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-902", "u902@example.com", "User 902"));
        String rawMcpToken = "mcp_recommend_token_902";

        McpToken mcpToken = McpToken.issue(
                member.getId(),
                mcpTokenHasher.hash(rawMcpToken),
                rawMcpToken.substring(0, Math.min(rawMcpToken.length(), 12)),
                "Codex",
                LocalDateTime.now().plusDays(3));
        mcpTokenRepository.save(mcpToken);

        mockMvc.perform(post("/api/v1/mcp/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(rawMcpToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keywords": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
