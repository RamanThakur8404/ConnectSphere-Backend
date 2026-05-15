package com.connectsphere.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.connectsphere.payment_service.config.RabbitMQConfig;
import com.connectsphere.payment_service.constant.Currency;
import com.connectsphere.payment_service.constant.PaymentStatus;
import com.connectsphere.payment_service.constant.PaymentType;
import com.connectsphere.payment_service.dto.AdminSummaryResponse;
import com.connectsphere.payment_service.dto.CreateOrderRequest;
import com.connectsphere.payment_service.dto.CreateOrderResponse;
import com.connectsphere.payment_service.dto.PaymentResponse;
import com.connectsphere.payment_service.dto.RefundRequest;
import com.connectsphere.payment_service.dto.SubscriptionStatusResponse;
import com.connectsphere.payment_service.dto.VerifyPaymentRequest;
import com.connectsphere.payment_service.entity.Payment;
import com.connectsphere.payment_service.exception.DuplicatePaymentException;
import com.connectsphere.payment_service.exception.PaymentNotFoundException;
import com.connectsphere.payment_service.exception.PaymentNotRefundableException;
import com.connectsphere.payment_service.exception.WebhookVerificationException;
import com.connectsphere.payment_service.repository.PaymentRepository;
import com.connectsphere.payment_service.service.impl.PaymentServiceImpl;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;

// Unit tests for {@link PaymentServiceImpl}.
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

	// -----------------------------------------------------------------------
	// Mocks
	// -----------------------------------------------------------------------

	@Mock
	private PaymentRepository paymentRepository;
	@Mock
	private RazorpayClient razorpayClient;
	@Mock
	private RabbitTemplate rabbitTemplate;
	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private ValueOperations<String, String> valueOperations;

	// Razorpay inner resource mocks
	@Mock
	com.razorpay.OrderClient ordersResource;
	@Mock
	com.razorpay.PaymentClient paymentsResource;

	@InjectMocks
	private PaymentServiceImpl paymentService;

	// -----------------------------------------------------------------------
	// Test data
	// -----------------------------------------------------------------------

	private static final Long USER_ID = 101L;
	private static final Long ADMIN_ID = 1L;
	private static final Long PAYMENT_ID = 500L;
	private static final String RAZORPAY_ORDER_ID = "order_ABC123";
	private static final String RAZORPAY_PAYMENT_ID = "pay_XYZ789";
	private static final BigDecimal AMOUNT = new BigDecimal("49900");

	@BeforeEach
	void setUp() {
		// Inject @Value fields manually (not injected by Mockito)
		ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_key");
		ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_key_secret");
		ReflectionTestUtils.setField(paymentService, "webhookSecret", "test_webhook_secret");

		// Wire up Razorpay sub-resource mocks
		razorpayClient.orders = ordersResource;
		razorpayClient.payments = paymentsResource;

		// Default: no idempotency key collision (only used by createOrder tests)
		lenient().when(redisTemplate.hasKey(anyString())).thenReturn(false);
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
	}

	// =========================================================================
	// createOrder() tests
	// =========================================================================

	@Test
	void createOrder_ValidRequest_ReturnsPendingOrderWithRazorpayId() throws RazorpayException {
		// Arrange
		CreateOrderRequest request = buildOrderRequest();
		Order mockOrder = mock(Order.class);
		when(mockOrder.get("id")).thenReturn(RAZORPAY_ORDER_ID);
		when(ordersResource.create(any())).thenReturn(mockOrder);

		Payment savedPayment = buildPayment(PaymentStatus.PENDING);
		when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

		// Act
		CreateOrderResponse response = paymentService.createOrder(USER_ID, request);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getRazorpayOrderId()).isEqualTo(RAZORPAY_ORDER_ID);
		assertThat(response.getRazorpayKeyId()).isEqualTo("rzp_test_key");
		assertThat(response.getAmount()).isEqualByComparingTo(AMOUNT);

		verify(paymentRepository).save(any(Payment.class));
		verify(valueOperations).set(anyString(), anyString(), any());
	}

	@Test
	void createOrder_DuplicateIdempotencyKey_ThrowsDuplicatePaymentException() {
		// Arrange — simulate Redis hit (key already exists)
		when(redisTemplate.hasKey(anyString())).thenReturn(true);

		CreateOrderRequest request = buildOrderRequest();

		// Act & Assert
		assertThatThrownBy(() -> paymentService.createOrder(USER_ID, request))
				.isInstanceOf(DuplicatePaymentException.class);

		verifyNoInteractions(paymentRepository);
	}

	@Test
	void createOrder_RazorpayApiFails_ThrowsRuntimeException() throws RazorpayException {
		// Arrange
		when(ordersResource.create(any())).thenThrow(new RazorpayException("API error"));

		// Act & Assert
		assertThatThrownBy(() -> paymentService.createOrder(USER_ID, buildOrderRequest()))
				.isInstanceOf(RuntimeException.class).hasMessageContaining("Razorpay");

		verify(paymentRepository, never()).save(any());
	}

	@Test
	void createOrder_InvalidRazorpayKeyId_ThrowsIllegalStateException() {
		// Arrange
		ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "undefined");

		// Act & Assert
		assertThatThrownBy(() -> paymentService.createOrder(USER_ID, buildOrderRequest()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Razorpay key id");

		verifyNoInteractions(ordersResource);
		verify(paymentRepository, never()).save(any());
	}

	@Test
	void createOrder_RedisUnavailable_ThrowsRedisConnectionFailureException() {
		// Arrange
		when(redisTemplate.hasKey(anyString())).thenThrow(new RedisConnectionFailureException("Unable to connect to Redis"));

		// Act & Assert
		assertThatThrownBy(() -> paymentService.createOrder(USER_ID, buildOrderRequest()))
				.isInstanceOf(RedisConnectionFailureException.class)
				.hasMessageContaining("Redis");

		verifyNoInteractions(ordersResource);
		verify(paymentRepository, never()).save(any());
	}

	// =========================================================================
	// getPaymentHistory() tests
	// =========================================================================

	@Test
	void getPaymentHistory_ValidUser_ReturnsPagedResults() {
		// Arrange
		Payment payment = buildPayment(PaymentStatus.SUCCESS);
		Page<Payment> paymentPage = new PageImpl<>(List.of(payment));
		when(paymentRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any())).thenReturn(paymentPage);

		// Act
		Page<PaymentResponse> result = paymentService.getPaymentHistory(USER_ID, PageRequest.of(0, 10));

		// Assert
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getUserId()).isEqualTo(USER_ID);
		assertThat(result.getContent().get(0).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
	}

	@Test
	void getPaymentHistory_NoTransactions_ReturnsEmptyPage() {
		// Arrange
		when(paymentRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any())).thenReturn(Page.empty());

		// Act
		Page<PaymentResponse> result = paymentService.getPaymentHistory(USER_ID, PageRequest.of(0, 10));

		// Assert
		assertThat(result.getContent()).isEmpty();
	}

	// =========================================================================
	// handleWebhook() tests
	// =========================================================================

	@Test
	void handleWebhook_PaymentCapturedEvent_UpdatesStatusToSuccess() throws RazorpayException {
		// Arrange — build a valid captured webhook payload
		String payload = buildWebhookPayload("payment.captured", RAZORPAY_ORDER_ID, RAZORPAY_PAYMENT_ID);
		String signature = "valid_signature";

		Payment pendingPayment = buildPayment(PaymentStatus.PENDING);
		pendingPayment.setRazorpayOrderId(RAZORPAY_ORDER_ID);

		when(paymentRepository.findByRazorpayOrderId(RAZORPAY_ORDER_ID)).thenReturn(Optional.of(pendingPayment));
		when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

		try (MockedStatic<com.razorpay.Utils> utilsMock = mockStatic(com.razorpay.Utils.class)) {
			utilsMock.when(() -> com.razorpay.Utils.verifyWebhookSignature(payload, signature, "test_webhook_secret"))
					.thenReturn(true);

			// Act
			paymentService.handleWebhook(payload, signature);
		}

		// Assert
		verify(paymentRepository).save(argThat(
				p -> p.getStatus() == PaymentStatus.SUCCESS && RAZORPAY_PAYMENT_ID.equals(p.getRazorpayPaymentId())));
		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.PAYMENT_EXCHANGE),
				eq(RabbitMQConfig.ROUTING_KEY_PAYMENT_SUCCESS), (Object) any());
	}

	@Test
	void handleWebhook_InvalidSignature_ThrowsWebhookVerificationException() {
		// Arrange
		String payload = buildWebhookPayload("payment.captured", RAZORPAY_ORDER_ID, RAZORPAY_PAYMENT_ID);
		String signature = "bad_signature";

		try (MockedStatic<com.razorpay.Utils> utilsMock = mockStatic(com.razorpay.Utils.class)) {
			utilsMock.when(() -> com.razorpay.Utils.verifyWebhookSignature(payload, signature, "test_webhook_secret"))
					.thenThrow(new RazorpayException("Signature mismatch"));

			// Act & Assert
			assertThatThrownBy(() -> paymentService.handleWebhook(payload, signature))
					.isInstanceOf(WebhookVerificationException.class);
		}

		verifyNoInteractions(paymentRepository);
	}

	@Test
	void handleWebhook_PaymentFailedEvent_UpdatesStatusToFailed() {
		// Arrange
		String payload = buildWebhookPayload("payment.failed", RAZORPAY_ORDER_ID, "");
		String signature = "valid_signature";

		Payment pendingPayment = buildPayment(PaymentStatus.PENDING);
		pendingPayment.setRazorpayOrderId(RAZORPAY_ORDER_ID);

		when(paymentRepository.findByRazorpayOrderId(RAZORPAY_ORDER_ID)).thenReturn(Optional.of(pendingPayment));
		when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

		try (MockedStatic<com.razorpay.Utils> utilsMock = mockStatic(com.razorpay.Utils.class)) {
			utilsMock.when(() -> com.razorpay.Utils.verifyWebhookSignature(payload, signature, "test_webhook_secret"))
					.thenReturn(true);

			// Act
			paymentService.handleWebhook(payload, signature);
		}

		// Assert
		verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.FAILED));
		verifyNoInteractions(rabbitTemplate);
	}

	@Test
	void verifyPayment_ValidSignature_FinalizesPaymentImmediately() throws RazorpayException {
		VerifyPaymentRequest request = new VerifyPaymentRequest();
		request.setRazorpayOrderId(RAZORPAY_ORDER_ID);
		request.setRazorpayPaymentId(RAZORPAY_PAYMENT_ID);
		request.setRazorpaySignature("sig_valid");

		Payment pendingPayment = buildPayment(PaymentStatus.PENDING);
		pendingPayment.setRazorpayOrderId(RAZORPAY_ORDER_ID);

		when(paymentRepository.findByRazorpayOrderId(RAZORPAY_ORDER_ID)).thenReturn(Optional.of(pendingPayment));
		when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

		try (MockedStatic<com.razorpay.Utils> utilsMock = mockStatic(com.razorpay.Utils.class)) {
			utilsMock.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(org.json.JSONObject.class),
					eq("test_key_secret"))).thenReturn(true);

			PaymentResponse response = paymentService.verifyPayment(USER_ID, request);

			assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
		}

		verify(paymentRepository).save(argThat(
				p -> p.getStatus() == PaymentStatus.SUCCESS && RAZORPAY_PAYMENT_ID.equals(p.getRazorpayPaymentId())));
		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.PAYMENT_EXCHANGE),
				eq(RabbitMQConfig.ROUTING_KEY_PAYMENT_SUCCESS), (Object) any());
	}

	@Test
	void verifyPayment_InvalidSignature_ThrowsWebhookVerificationException() throws RazorpayException {
		VerifyPaymentRequest request = new VerifyPaymentRequest();
		request.setRazorpayOrderId(RAZORPAY_ORDER_ID);
		request.setRazorpayPaymentId(RAZORPAY_PAYMENT_ID);
		request.setRazorpaySignature("sig_invalid");

		try (MockedStatic<com.razorpay.Utils> utilsMock = mockStatic(com.razorpay.Utils.class)) {
			utilsMock.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(org.json.JSONObject.class),
					eq("test_key_secret"))).thenThrow(new RazorpayException("Signature mismatch"));

			assertThatThrownBy(() -> paymentService.verifyPayment(USER_ID, request))
					.isInstanceOf(WebhookVerificationException.class);
		}

		verifyNoInteractions(paymentRepository);
	}

	// =========================================================================
	// processRefund() tests
	// =========================================================================

	@Test
	void processRefund_SuccessPayment_RefundsAndPublishesEvent() throws RazorpayException {
		// Arrange
		Payment successPayment = buildPayment(PaymentStatus.SUCCESS);
		successPayment.setRazorpayPaymentId(RAZORPAY_PAYMENT_ID);

		when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(successPayment));

		Refund mockRefund = mock(Refund.class);
		when(mockRefund.get("id")).thenReturn("rfnd_TEST123");
		when(paymentsResource.refund(eq(RAZORPAY_PAYMENT_ID), any())).thenReturn(mockRefund);
		when(paymentRepository.save(any(Payment.class))).thenReturn(successPayment);

		RefundRequest refundRequest = new RefundRequest();
		refundRequest.setRefundNote("Test refund by admin");

		// Act
		PaymentResponse response = paymentService.processRefund(PAYMENT_ID, refundRequest, ADMIN_ID);

		// Assert
		assertThat(response).isNotNull();
		verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.REFUNDED));
		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.PAYMENT_EXCHANGE),
				eq(RabbitMQConfig.ROUTING_KEY_PAYMENT_REFUNDED), (Object) any());
	}

	@Test
	void processRefund_PaymentNotFound_ThrowsPaymentNotFoundException() {
		// Arrange
		when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> paymentService.processRefund(PAYMENT_ID, new RefundRequest(), ADMIN_ID))
				.isInstanceOf(PaymentNotFoundException.class);
	}

	@Test
	void processRefund_PendingPayment_ThrowsPaymentNotRefundableException() {
		// Arrange — only SUCCESS payments can be refunded
		Payment pendingPayment = buildPayment(PaymentStatus.PENDING);
		when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pendingPayment));

		// Act & Assert
		assertThatThrownBy(() -> paymentService.processRefund(PAYMENT_ID, new RefundRequest(), ADMIN_ID))
				.isInstanceOf(PaymentNotRefundableException.class);
	}

	@Test
	void processRefund_RefundAmountExceedsOriginal_ThrowsIllegalArgumentException() {
		// Arrange
		Payment successPayment = buildPayment(PaymentStatus.SUCCESS); // amount = 49900
		successPayment.setRazorpayPaymentId(RAZORPAY_PAYMENT_ID);
		when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(successPayment));

		RefundRequest request = new RefundRequest();
		request.setRefundAmount(new BigDecimal("99999")); // exceeds 49900
		request.setRefundNote("Overcharge refund");

		// Act & Assert
		assertThatThrownBy(() -> paymentService.processRefund(PAYMENT_ID, request, ADMIN_ID))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void processRefund_SuccessPaymentWithoutRazorpayPaymentId_ThrowsIllegalArgumentException() {
		// Arrange
		Payment successPayment = buildPayment(PaymentStatus.SUCCESS);
		when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(successPayment));

		// Act & Assert
		assertThatThrownBy(() -> paymentService.processRefund(PAYMENT_ID, new RefundRequest(), ADMIN_ID))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Razorpay payment id");

		verifyNoInteractions(paymentsResource);
	}

	@Test
	void approvePayment_PendingPayment_MarksSuccessAndPublishesEvent() {
		// Arrange
		Payment pendingPayment = buildPayment(PaymentStatus.PENDING);
		when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pendingPayment));
		when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

		// Act
		PaymentResponse response = paymentService.approvePayment(PAYMENT_ID, ADMIN_ID);

		// Assert
		assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
		verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.SUCCESS));
		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.PAYMENT_EXCHANGE),
				eq(RabbitMQConfig.ROUTING_KEY_PAYMENT_SUCCESS), (Object) any());
	}

	@Test
	void approvePayment_RefundedPayment_ThrowsIllegalArgumentException() {
		// Arrange
		Payment refundedPayment = buildPayment(PaymentStatus.REFUNDED);
		when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(refundedPayment));

		// Act & Assert
		assertThatThrownBy(() -> paymentService.approvePayment(PAYMENT_ID, ADMIN_ID))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("pending payments");
	}

	@Test
	void cancelPayment_PendingPayment_MarksCancelledWithoutPublishingEvent() {
		Payment pendingPayment = buildPayment(PaymentStatus.PENDING);
		when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pendingPayment));
		when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

		PaymentResponse response = paymentService.cancelPayment(PAYMENT_ID, ADMIN_ID);

		assertThat(response.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
		verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.CANCELLED));
		verify(rabbitTemplate, never()).convertAndSend(eq(RabbitMQConfig.PAYMENT_EXCHANGE),
				eq(RabbitMQConfig.ROUTING_KEY_PAYMENT_SUCCESS), (Object) any());
	}

	@Test
	void cancelPayment_SuccessPayment_ThrowsIllegalArgumentException() {
		Payment successPayment = buildPayment(PaymentStatus.SUCCESS);
		when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(successPayment));

		assertThatThrownBy(() -> paymentService.cancelPayment(PAYMENT_ID, ADMIN_ID))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("pending payments");

		verify(paymentRepository, never()).save(any(Payment.class));
	}

	// =========================================================================
	// getSubscriptionStatus() tests
	// =========================================================================

	@Test
	void getSubscriptionStatus_ActiveSubscription_ReturnsActiveStatus() {
		// Arrange
		Payment sub = buildPayment(PaymentStatus.SUCCESS);
		sub.setPaymentType(PaymentType.SUBSCRIPTION);
		when(paymentRepository.findTopByUserIdAndPaymentTypeAndStatusOrderByCreatedAtDesc(USER_ID,
				PaymentType.SUBSCRIPTION, PaymentStatus.SUCCESS)).thenReturn(Optional.of(sub));

		// Act
		SubscriptionStatusResponse response = paymentService.getSubscriptionStatus(USER_ID);

		// Assert
		assertThat(response.isActive()).isTrue();
		assertThat(response.getPaymentId()).isEqualTo(PAYMENT_ID);
	}

	@Test
	void getSubscriptionStatus_NoSubscription_ReturnsInactiveStatus() {
		// Arrange
		when(paymentRepository.findTopByUserIdAndPaymentTypeAndStatusOrderByCreatedAtDesc(any(), any(), any()))
				.thenReturn(Optional.empty());

		// Act
		SubscriptionStatusResponse response = paymentService.getSubscriptionStatus(USER_ID);

		// Assert
		assertThat(response.isActive()).isFalse();
	}

	// =========================================================================
	// getAdminSummary() tests
	// =========================================================================

	@Test
	void getAdminSummary_ReturnsCorrectAggregates() {
		// Arrange
		when(paymentRepository.calculateTotalRevenue()).thenReturn(new BigDecimal("100000"));
		when(paymentRepository.calculateTotalRefunded()).thenReturn(new BigDecimal("5000"));
		when(paymentRepository.count()).thenReturn(50L);
		when(paymentRepository.countByStatus(PaymentStatus.SUCCESS)).thenReturn(40L);
		when(paymentRepository.countByStatus(PaymentStatus.FAILED)).thenReturn(5L);
		when(paymentRepository.countByStatus(PaymentStatus.REFUNDED)).thenReturn(5L);
		when(paymentRepository.countByPaymentTypeAndStatus(PaymentType.SUBSCRIPTION, PaymentStatus.SUCCESS))
				.thenReturn(20L);

		// Act
		AdminSummaryResponse response = paymentService.getAdminSummary();

		// Assert
		assertThat(response.getTotalRevenue()).isEqualByComparingTo("100000");
		assertThat(response.getTotalRefunded()).isEqualByComparingTo("5000");
		assertThat(response.getNetRevenue()).isEqualByComparingTo("95000");
		assertThat(response.getSuccessfulTransactions()).isEqualTo(40L);
		assertThat(response.getActiveSubscriptions()).isEqualTo(20L);
	}

	@Test
	void getAllPayments_ReturnsPagedResults() {
		// Arrange
		Payment payment = buildPayment(PaymentStatus.SUCCESS);
		PageRequest pageable = PageRequest.of(0, 20);
		when(paymentRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(payment)));

		// Act
		Page<PaymentResponse> result = paymentService.getAllPayments(pageable);

		// Assert
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getPaymentId()).isEqualTo(PAYMENT_ID);
		assertThat(result.getContent().get(0).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
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

	private Payment buildPayment(PaymentStatus status) {
		return Payment.builder().paymentId(PAYMENT_ID).userId(USER_ID).razorpayOrderId(RAZORPAY_ORDER_ID).amount(AMOUNT)
				.currency(Currency.INR).paymentType(PaymentType.SUBSCRIPTION).status(status)
				.description("Monthly subscription").createdAt(LocalDateTime.now()).build();
	}

	private String buildWebhookPayload(String event, String orderId, String paymentId) {
		return String.format("""
				{
				  "event": "%s",
				  "payload": {
				    "payment": {
				      "entity": {
				        "id": "%s",
				        "order_id": "%s"
				      }
				    }
				  }
				}
				""", event, paymentId, orderId);
	}
}
