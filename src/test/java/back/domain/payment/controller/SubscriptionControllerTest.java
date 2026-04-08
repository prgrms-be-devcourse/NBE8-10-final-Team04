package back.domain.payment.controller;

import back.domain.payment.dto.response.SubscriptionStatusResponse;
import back.domain.payment.entity.Subscription;
import back.domain.payment.entity.SubscriptionStatus;
import back.domain.payment.service.SubscriptionService;
import back.domain.payment.service.UsageService;
import back.global.security.AuthenticatedMember;
import back.testUtil.WithMockMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsageService usageService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        AuthenticatedMember mockMember = new AuthenticatedMember(1L, "ROLE_USER");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                mockMember,
                null,
                mockMember.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("무료 사용 조회 성공")
    @WithMockMember
    void getUsage() throws Exception {
        given(usageService.getRemainingUsage(1L)).willReturn(2);

        mockMvc.perform(get("/api/subscriptions/me/usage"))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    @DisplayName("무료 사용 1회 차감 성공")
    @WithMockMember
    void useOnce() throws Exception {
        given(usageService.useOnce(1L)).willReturn(2); // willDoNothing → given으로 변경

        mockMvc.perform(post("/api/subscriptions/me/usage")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"1회 사용 완료하였습니다.\",\"remaining\":2}"));
    }

    @Test
    @DisplayName("구독 상태 조회 성공")
    @WithMockMember
    void getSubscription() throws Exception {
        given(subscriptionService.getStatus(1L))
                .willReturn(new SubscriptionStatusResponse(SubscriptionStatus.ACTIVE, "구독 중"));

        mockMvc.perform(get("/api/subscriptions/me"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"ACTIVE\",\"message\":\"구독 중\"}"));
    }

    @Test
    @WithMockMember // Spring Security 가짜 유저 주입
    @DisplayName("구독 해지 API 성공 - 200 OK와 해지 메시지를 반환한다")
    void cancelSubscription_Success() throws Exception {
        // given
        // 서비스가 엔티티를 반환하므로 테스트용 가짜 객체 생성
        Subscription mockSubscription = Mockito.mock(Subscription.class);
        given(mockSubscription.getNextBillingAt()).willReturn(LocalDateTime.now().plusDays(30));
        given(mockSubscription.getStatus()).willReturn(SubscriptionStatus.CANCELED);

        given(subscriptionService.cancelWithRefund(anyLong()))
                .willReturn(false);

        given(subscriptionService.findByMemberId(anyLong()))
                .willReturn(mockSubscription);

        // when & then
        mockMvc.perform(post("/api/subscriptions/me/cancel")
                        .with(csrf()) // 스프링 시큐리티 CSRF 토큰 대응
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("혜택 유지 후")))
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.nextBillingAt").exists());
    }

    @Test
    @WithMockMember
    @DisplayName("구독 중이 아닐 때 해지 요청 시 - 400 에러를 반환한다")
    void cancelSubscription_Fail_NoActiveSubscription() throws Exception {
        // given
        given(subscriptionService.cancelWithRefund(anyLong()))
                .willReturn(false);

        given(subscriptionService.findByMemberId(anyLong()))
                .willThrow(new IllegalArgumentException("해지할 수 있는 활성 구독권이 없습니다."));

        // when & then
        mockMvc.perform(post("/api/subscriptions/me/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()); // GlobalExceptionHandler가 400으로 변환한다고 가정
    }
}