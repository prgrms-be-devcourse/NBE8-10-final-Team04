package back.domain.payment.controller;

import back.domain.payment.dto.response.PaymentPrepareResponse;
import back.domain.payment.entity.SubscriptionPlanType;
import back.domain.payment.service.PaymentService;
import back.testUtil.WithMockMember;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(PaymentController.class)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;;

    @Test
    @WithMockMember
    void 결제준비_응답_JSON_형식_확인() throws Exception {
        String orderId = "SUB-123e4567-e89b-12d3-a456-426614174000";
        PaymentPrepareResponse response = new PaymentPrepareResponse(
                orderId, "월간 구독", 990, SubscriptionPlanType.MONTHLY_990
        );

        given(paymentService.prepare(anyLong(), any())).willReturn(response);

        // [핵심] perform 괄호와 마침표 위치를 잘 보세요!
        mockMvc.perform(post("/api/payments/prepare") // 경로 앞에 / 추가
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planType\": \"MONTHLY_990\"}")
                        .with(csrf())) // 여기서 perform 괄호가 닫힙니다.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.orderName").value("월간 구독"))
                .andExpect(jsonPath("$.amount").value(990))
                .andExpect(jsonPath("$.planType").value("MONTHLY_990"));
    }
}
