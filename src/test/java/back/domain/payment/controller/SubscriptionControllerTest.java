package back.domain.payment.controller;

import back.domain.payment.entity.SubscriptionStatus;
import back.domain.payment.service.SubscriptionService;
import back.domain.payment.service.UsageService;
import back.testUtil.WithMockMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsageService usageService;

    @MockitoBean
    private SubscriptionService subscriptionService;

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
        willDoNothing().given(usageService).useOnce(1L);

        mockMvc.perform(post("/api/subscriptions/me/usage")
                        .with(csrf()))
                        .andExpect(status().isOk());
    }

    @Test
    @DisplayName("구독 상태 조회 성공")
    @WithMockMember
    void getSubscription() throws Exception {
        given(subscriptionService.getStatus(1L)).willReturn(SubscriptionStatus.ACTIVE);

        mockMvc.perform(get("/api/subscriptions/me"))
                .andExpect(status().isOk())
                .andExpect(content().json("\"ACTIVE\""));
    }
}