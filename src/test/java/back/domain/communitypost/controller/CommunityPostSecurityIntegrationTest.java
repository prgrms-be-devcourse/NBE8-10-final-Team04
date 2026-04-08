package back.domain.communitypost.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.global.security.JwtTokenProvider;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CommunityPostSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("공개 게시글 목록 조회는 토큰 없이 접근 가능하다")
    void getPublicPosts_withoutToken_success() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글 목록 조회 성공"));
    }

    @Test
    @DisplayName("관리자 게시글 생성 API는 인증이 없으면 401을 반환한다")
    void generatePost_withoutAuth_unauthorized() throws Exception {
        mockMvc.perform(
                        post("/api/v1/admin/community/posts/generate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "type": "MODEL_INFO",
                                  "targetDate": "2026-04-08"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("관리자 게시글 생성 API는 USER 권한이면 403을 반환한다")
    void generatePost_withUserRole_forbidden() throws Exception {
        Member user =
                memberRepository.save(Member.createUser("sub-community-user", "cuser@example.com", "Community User"));
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        mockMvc.perform(
                        post("/api/v1/admin/community/posts/generate")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "type": "MODEL_INFO",
                                  "targetDate": "2026-04-08"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
    }
}
