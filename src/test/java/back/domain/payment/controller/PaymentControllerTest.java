package back.domain.payment.controller;

import back.domain.payment.dto.request.PaymentConfirmRequest;
import back.domain.payment.dto.response.PaymentPrepareResponse;
import back.domain.payment.entity.Payment;
import back.domain.payment.entity.SubscriptionPlanType;
import back.domain.payment.service.PaymentService;
import back.global.security.AuthenticatedMember;
import back.domain.payment.entity.PaymentStatus;
import back.testUtil.WithMockMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

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

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 인증 객체 주입 (memberId: 1L)
        AuthenticatedMember mockMember = new AuthenticatedMember(1L, "ROLE_USER");
        Authentication auth = new UsernamePasswordAuthenticationToken(mockMember, null, mockMember.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

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

    @Test
    @DisplayName("결제 승인 성공 - 200 OK와 승인 정보를 반환한다")
    void confirmPayment_Success() throws Exception {
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "toss_key_123",
                "order_id_456",
                990L
        );

        // 서비스에서 결제 엔티티를 반환한다고 가정 (레코드가 아니면 Mockito.mock 사용)
        Payment mockPayment = Mockito.mock(Payment.class);
        given(mockPayment.getOrderId()).willReturn("order_id_456");
        given(mockPayment.getAmount()).willReturn(990);
        given(mockPayment.getStatus()).willReturn(PaymentStatus.PAID);

        given(paymentService.confirm(anyLong(), any(PaymentConfirmRequest.class)))
                .willReturn(mockPayment);

        // when & then
        mockMvc.perform(post("/api/payments/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order_id_456"))
                .andExpect(jsonPath("$.status").value("PAID"));
    }
}
