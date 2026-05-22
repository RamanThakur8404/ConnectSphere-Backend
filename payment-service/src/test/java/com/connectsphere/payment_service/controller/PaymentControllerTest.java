package com.connectsphere.payment_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.payment_service.config.SecurityConfig;
import com.connectsphere.payment_service.constant.Currency;
import com.connectsphere.payment_service.constant.PaymentStatus;
import com.connectsphere.payment_service.constant.PaymentType;
import com.connectsphere.payment_service.dto.AdminSummaryResponse;
import com.connectsphere.payment_service.dto.CreateOrderRequest;
import com.connectsphere.payment_service.dto.CreateOrderResponse;
import com.connectsphere.payment_service.dto.PaymentResponse;
import com.connectsphere.payment_service.dto.SubscriptionStatusResponse;
import com.connectsphere.payment_service.security.HeaderAuthFilter;
import com.connectsphere.payment_service.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;

// REST layer tests for {@link PaymentController}.
@WebMvcTest(PaymentController.class)
@Import({ SecurityConfig.class, HeaderAuthFilter.class })
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private static final Long    USER_ID        = 101L;
    private static final String  RAZORPAY_ORDER = "order_ABC123";
    private static final BigDecimal AMOUNT      = new BigDecimal("49900");

    // =========================================================================
    // POST /api/v1/payments/order — Create Razorpay Order
    // =========================================================================

    @Test
    @WithMockUser(username = "101", roles = "USER")
    void createOrder_ValidRequest_Returns201WithOrderId() throws Exception {
        // Arrange
        CreateOrderRequest request = buildOrderRequest();
        CreateOrderResponse response = CreateOrderResponse.builder()
                .paymentId(500L)
                .razorpayOrderId(RAZORPAY_ORDER)
                .razorpayKeyId("rzp_test_key")
                .amount(AMOUNT)
                .currency(Currency.INR)
                .paymentType(PaymentType.SUBSCRIPTION)
                .build();

        when(paymentService.createOrder(any(), any(CreateOrderRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                .header("X-User-Id", "101")
                .header("X-User-Role", "USER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.razorpayOrderId").value(RAZORPAY_ORDER))
                .andExpect(jsonPath("$.data.razorpayKeyId").value("rzp_test_key"))
                .andExpect(jsonPath("$.data.paymentId").value(500));
    }

    @Test
    void createOrder_NoAuthentication_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/payments/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildOrderRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "101", roles = "USER")
    void createOrder_MissingAmount_Returns400ValidationError() throws Exception {
        // Arrange — amount is null (required field)
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCurrency(Currency.INR);
        request.setPaymentType(PaymentType.SUBSCRIPTION);

        mockMvc.perform(post("/api/v1/payments/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", "101")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // =========================================================================
    // GET /api/v1/payments/history — Payment History
    // =========================================================================

    @Test
    @WithMockUser(username = "101", roles = "USER")
    void getPaymentHistory_ValidUser_Returns200WithPage() throws Exception {
        // Arrange
        PaymentResponse paymentResponse = buildPaymentResponse(PaymentStatus.SUCCESS);
        Page<PaymentResponse> page = new PageImpl<>(List.of(paymentResponse));
        when(paymentService.getPaymentHistory(any(), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/history")
                .header("X-User-Id", "101")
                .header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].userId").value(USER_ID));
    }

    @Test
    void getPaymentHistory_Unauthenticated_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/payments/history"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/v1/payments/subscription/status — Subscription Status
    // =========================================================================

    @Test
    @WithMockUser(username = "101", roles = "USER")
    void getSubscriptionStatus_ActiveSubscription_Returns200() throws Exception {
        // Arrange
        SubscriptionStatusResponse response = SubscriptionStatusResponse.builder()
                .active(true)
                .paymentId(500L)
                .amount(AMOUNT)
                .currency("INR")
                .subscribedAt(LocalDateTime.now())
                .renewalDue(LocalDateTime.now().plusDays(30))
                .build();

        when(paymentService.getSubscriptionStatus(any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/subscription/status")
                .header("X-User-Id", "101")
                .header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    // =========================================================================
    // POST /api/v1/payments/webhook — Razorpay Webhook (PUBLIC)
    // =========================================================================

    @Test
    void handleWebhook_ValidSignature_Returns200() throws Exception {
        // Arrange — webhook is PUBLIC (no auth required)
        String payload   = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_123\",\"order_id\":\"order_ABC\"}}}}";
        String signature = "valid_hmac_signature";

        // paymentService.handleWebhook() does nothing (void, no exception)

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Razorpay-Signature", signature))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully"));
    }

    @Test
    void handleWebhook_NoAuthentication_Still200BecauseItIsPublic() throws Exception {
        // Webhook endpoint must be accessible without any auth headers
        String payload = "{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"\",\"order_id\":\"order_ABC\"}}}}";

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Razorpay-Signature", "sig"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // POST /api/v1/payments/admin/refund/{id} — Admin Refund
    // =========================================================================

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void processRefund_AdminRole_Returns200() throws Exception {
        // Arrange
        PaymentResponse refunded = buildPaymentResponse(PaymentStatus.REFUNDED);
        when(paymentService.processRefund(any(), any(), any())).thenReturn(refunded);

        String body = """
            { "refundNote": "Customer requested refund" }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/admin/refund/500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
    }

    @Test
    @WithMockUser(username = "101", roles = "USER")
    void processRefund_UserRole_Returns403() throws Exception {
        // Regular users cannot access admin endpoints
        mockMvc.perform(post("/api/v1/payments/admin/refund/500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refundNote\":\"test\"}")
                        .header("X-User-Id", "101")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/v1/payments/admin/summary — Admin Summary
    // =========================================================================

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void approvePayment_AdminRole_Returns200WithSuccessPayment() throws Exception {
        // Arrange
        PaymentResponse approved = buildPaymentResponse(PaymentStatus.SUCCESS);
        when(paymentService.approvePayment(any(), any())).thenReturn(approved);

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/admin/approve/500")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @WithMockUser(username = "101", roles = "USER")
    void approvePayment_UserRole_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/payments/admin/approve/500")
                        .header("X-User-Id", "101")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void cancelPayment_AdminRole_Returns200WithCancelledPayment() throws Exception {
        PaymentResponse cancelled = buildPaymentResponse(PaymentStatus.CANCELLED);
        when(paymentService.cancelPayment(any(), any())).thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/payments/admin/cancel/500")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "101", roles = "USER")
    void cancelPayment_UserRole_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/payments/admin/cancel/500")
                        .header("X-User-Id", "101")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void getAdminSummary_AdminRole_Returns200WithSummary() throws Exception {
        // Arrange
        AdminSummaryResponse summary = AdminSummaryResponse.builder()
                .totalRevenue(new BigDecimal("1000000"))
                .totalRefunded(new BigDecimal("50000"))
                .netRevenue(new BigDecimal("950000"))
                .totalTransactions(200L)
                .successfulTransactions(180L)
                .failedTransactions(15L)
                .refundedTransactions(5L)
                .activeSubscriptions(90L)
                .build();

        when(paymentService.getAdminSummary()).thenReturn(summary);

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/admin/summary")
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.netRevenue").value(950000))
                .andExpect(jsonPath("$.data.activeSubscriptions").value(90));
    }

    @Test
    void getAdminSummary_Unauthenticated_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/payments/admin/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void getAllPayments_AdminRole_ReturnsPagedTransactions() throws Exception {
        // Arrange
        Page<PaymentResponse> page = new PageImpl<>(List.of(buildPaymentResponse(PaymentStatus.SUCCESS)));
        when(paymentService.getAllPayments(any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/admin/history")
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].paymentId").value(500))
                .andExpect(jsonPath("$.data.content[0].status").value("SUCCESS"));
    }

    // =========================================================================
    // Test data builders
    // =========================================================================

    private CreateOrderRequest buildOrderRequest() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setAmount(AMOUNT);
        req.setCurrency(Currency.INR);
        req.setPaymentType(PaymentType.SUBSCRIPTION);
        req.setDescription("Monthly subscription");
        return req;
    }

    private PaymentResponse buildPaymentResponse(PaymentStatus status) {
        return PaymentResponse.builder()
                .paymentId(500L)
                .userId(USER_ID)
                .razorpayOrderId(RAZORPAY_ORDER)
                .amount(AMOUNT)
                .currency(Currency.INR)
                .paymentType(PaymentType.SUBSCRIPTION)
                .status(status)
                .description("Monthly subscription")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
