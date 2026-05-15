package com.connectsphere.payment_service.service.impl;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.payment_service.config.RabbitMQConfig;
import com.connectsphere.payment_service.constant.ErrorMessages;
import com.connectsphere.payment_service.constant.LogMessages;
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
import com.connectsphere.payment_service.event.PaymentRefundedEvent;
import com.connectsphere.payment_service.event.PaymentSuccessEvent;
import com.connectsphere.payment_service.exception.DuplicatePaymentException;
import com.connectsphere.payment_service.exception.PaymentNotFoundException;
import com.connectsphere.payment_service.exception.PaymentNotRefundableException;
import com.connectsphere.payment_service.exception.WebhookVerificationException;
import com.connectsphere.payment_service.mapper.PaymentMapper;
import com.connectsphere.payment_service.repository.PaymentRepository;
import com.connectsphere.payment_service.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;

// Core implementation of PaymentService.
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

	private final PaymentRepository paymentRepository;
	private final RazorpayClient razorpayClient;
	private final RabbitTemplate rabbitTemplate;
	private final StringRedisTemplate redisTemplate;

	@Value("${razorpay.key.id}")
	private String razorpayKeyId;

	@Value("${razorpay.key.secret}")
	private String razorpayKeySecret;

	@Value("${razorpay.webhook.secret}")
	private String webhookSecret;

	// Idempotency key TTL: 5 minutes prevents double-charge on network retries.
	private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(5);

	// Redis key prefix for idempotency tracking.
	private static final String IDEMPOTENCY_PREFIX = "payment:idempotency:";

	// -----------------------------------------------------------------------
	// Create Razorpay Order
	// -----------------------------------------------------------------------

	@Override
	@Transactional
	public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request) {
		log.info(LogMessages.ORDER_CREATE_ATTEMPT, userId, request.getAmount(), request.getPaymentType());

		String checkoutKeyId = requireValidCheckoutKeyId();
		String idempotencyKey = buildIdempotencyKey(userId, request);
		if (Boolean.TRUE.equals(redisTemplate.hasKey(IDEMPOTENCY_PREFIX + idempotencyKey))) {
			log.warn("Duplicate payment order detected - userId: {}, key: {}", userId, idempotencyKey);
			throw new DuplicatePaymentException(ErrorMessages.DUPLICATE_IDEMPOTENCY_KEY);
		}

		Order razorpayOrder;
		try {
			JSONObject orderRequest = new JSONObject();
			orderRequest.put("amount", request.getAmount().longValue());
			orderRequest.put("currency", request.getCurrency().name());
			orderRequest.put("receipt", "rcpt_" + userId + "_" + System.currentTimeMillis());

			JSONObject notes = new JSONObject();
			notes.put("userId", userId.toString());
			notes.put("paymentType", request.getPaymentType().name());
			orderRequest.put("notes", notes);

			razorpayOrder = razorpayClient.orders.create(orderRequest);

		} catch (RazorpayException e) {
			log.error(LogMessages.ORDER_CREATE_FAILED, userId, e.getMessage());
			throw new RuntimeException(ErrorMessages.RAZORPAY_ORDER_FAILED);
		}

		String razorpayOrderId = razorpayOrder.get("id");

		Payment payment = Payment.builder().userId(userId).razorpayOrderId(razorpayOrderId)
				.idempotencyKey(idempotencyKey).amount(request.getAmount()).currency(request.getCurrency())
				.paymentType(request.getPaymentType()).status(PaymentStatus.PENDING)
				.description(request.getDescription()).build();

		Payment saved = paymentRepository.save(payment);

		redisTemplate.opsForValue().set(IDEMPOTENCY_PREFIX + idempotencyKey, saved.getPaymentId().toString(),
				IDEMPOTENCY_TTL);

		log.info(LogMessages.ORDER_CREATE_SUCCESS, razorpayOrderId, saved.getPaymentId());

		return CreateOrderResponse.builder().paymentId(saved.getPaymentId()).razorpayOrderId(razorpayOrderId)
				.razorpayKeyId(checkoutKeyId).amount(request.getAmount()).currency(request.getCurrency())
				.paymentType(request.getPaymentType()).description(request.getDescription()).build();
	}

	// -----------------------------------------------------------------------
	// Payment History
	// -----------------------------------------------------------------------

	@Override
	public Page<PaymentResponse> getPaymentHistory(Long userId, Pageable pageable) {
		log.info(LogMessages.HISTORY_FETCH, userId, pageable.getPageNumber());
		return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(PaymentMapper::toResponse);
	}

	@Override
	public Page<PaymentResponse> getAllPayments(Pageable pageable) {
		log.info("Fetch all payments | page={}", pageable.getPageNumber());
		return paymentRepository.findAll(pageable).map(PaymentMapper::toResponse);
	}

	// -----------------------------------------------------------------------
	// Razorpay Webhook Handler
	// -----------------------------------------------------------------------

	@Override
	@Transactional
	public void handleWebhook(String payload, String signature) {
		log.info(LogMessages.WEBHOOK_RECEIVED, "incoming");

		verifyWebhookSignature(payload, signature);
		log.info(LogMessages.WEBHOOK_SIGNATURE_VALID);

		JSONObject webhookBody = new JSONObject(payload);
		String event = webhookBody.optString("event", "");

		log.info(LogMessages.WEBHOOK_RECEIVED, event);

		JSONObject paymentEntity = webhookBody.optJSONObject("payload") != null
				? webhookBody.getJSONObject("payload").optJSONObject("payment") != null
						? webhookBody.getJSONObject("payload").getJSONObject("payment").optJSONObject("entity")
						: null
				: null;

		if (paymentEntity == null) {
			log.warn(LogMessages.WEBHOOK_EVENT_IGNORED, event);
			return;
		}

		String razorpayOrderId = paymentEntity.optString("order_id", "");
		String razorpayPaymentId = paymentEntity.optString("id", "");

		switch (event) {
		case "payment.captured" -> handlePaymentCaptured(razorpayOrderId, razorpayPaymentId);
		case "payment.failed" -> handlePaymentFailed(razorpayOrderId);
		default -> log.info(LogMessages.WEBHOOK_EVENT_IGNORED, event);
		}
	}

	@Override
	@Transactional
	public PaymentResponse verifyPayment(Long userId, VerifyPaymentRequest request) {
		log.info("Verifying checkout payment | userId={}, orderId={}", userId, request.getRazorpayOrderId());

		verifyCheckoutSignature(request);

		Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId()).orElseThrow(
				() -> new PaymentNotFoundException(ErrorMessages.PAYMENT_NOT_FOUND_ORDER + request.getRazorpayOrderId()));

		if (!userId.equals(payment.getUserId())) {
			throw new IllegalArgumentException(ErrorMessages.UNAUTHORIZED_ACCESS);
		}

		finalizeSuccessfulPayment(payment, request.getRazorpayPaymentId());
		return PaymentMapper.toResponse(payment);
	}

	// -----------------------------------------------------------------------
	// Subscription Status
	// -----------------------------------------------------------------------

	@Override
	public SubscriptionStatusResponse getSubscriptionStatus(Long userId) {
		log.info(LogMessages.SUBSCRIPTION_STATUS_FETCH, userId);

		return paymentRepository
				.findTopByUserIdAndPaymentTypeAndStatusOrderByCreatedAtDesc(userId, PaymentType.SUBSCRIPTION,
						PaymentStatus.SUCCESS)
				.map(p -> SubscriptionStatusResponse.builder().active(true).paymentId(p.getPaymentId())
						.amount(p.getAmount()).currency(p.getCurrency().name()).subscribedAt(p.getCreatedAt())
						.renewalDue(p.getCreatedAt().plusDays(30)).build())
				.orElse(SubscriptionStatusResponse.builder().active(false).build());
	}

	// -----------------------------------------------------------------------
	// Admin: Process Refund
	// -----------------------------------------------------------------------

	@Override
	@Transactional
	public PaymentResponse processRefund(Long paymentId, RefundRequest request, Long adminUserId) {
		log.info(LogMessages.REFUND_ATTEMPT, paymentId, adminUserId);

		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new PaymentNotFoundException(ErrorMessages.PAYMENT_NOT_FOUND + paymentId));

		if (payment.getStatus() != PaymentStatus.SUCCESS) {
			throw new PaymentNotRefundableException(ErrorMessages.REFUND_NOT_SUCCESS_STATUS);
		}

		if (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isBlank()) {
			throw new IllegalArgumentException(ErrorMessages.REFUND_PAYMENT_ID_REQUIRED);
		}

		BigDecimal refundAmount = (request.getRefundAmount() != null) ? request.getRefundAmount() : payment.getAmount();

		if (refundAmount.compareTo(payment.getAmount()) > 0) {
			throw new IllegalArgumentException(ErrorMessages.REFUND_AMOUNT_EXCEEDS);
		}

		String razorpayRefundId;
		try {
			JSONObject refundRequest = new JSONObject();
			refundRequest.put("amount", refundAmount.longValue());

			Refund refund = razorpayClient.payments.refund(payment.getRazorpayPaymentId(), refundRequest);
			razorpayRefundId = refund.get("id");

		} catch (RazorpayException e) {
			log.error(LogMessages.REFUND_FAILED, paymentId, e.getMessage());
			throw new RuntimeException(ErrorMessages.RAZORPAY_REFUND_FAILED);
		}

		payment.setStatus(PaymentStatus.REFUNDED);
		payment.setRazorpayRefundId(razorpayRefundId);
		payment.setRefundNote(request.getRefundNote());
		payment.setUpdatedAt(LocalDateTime.now());

		Payment updated = paymentRepository.save(payment);

		log.info(LogMessages.REFUND_SUCCESS, paymentId, razorpayRefundId);
		publishRefundedEvent(updated, refundAmount);

		return PaymentMapper.toResponse(updated);
	}

	// -----------------------------------------------------------------------
	// Admin: Approve Pending Payment
	// -----------------------------------------------------------------------

	@Override
	@Transactional
	public PaymentResponse approvePayment(Long paymentId, Long adminUserId) {
		log.info("Payment approval requested | paymentId={}, adminId={}", paymentId, adminUserId);

		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new PaymentNotFoundException(ErrorMessages.PAYMENT_NOT_FOUND + paymentId));

		if (payment.getStatus() == PaymentStatus.SUCCESS) {
			return PaymentMapper.toResponse(payment);
		}

		if (payment.getStatus() != PaymentStatus.PENDING) {
			throw new IllegalArgumentException(ErrorMessages.PAYMENT_APPROVAL_INVALID_STATUS);
		}

		payment.setStatus(PaymentStatus.SUCCESS);
		payment.setUpdatedAt(LocalDateTime.now());

		Payment updated = paymentRepository.save(payment);
		publishSuccessEvent(updated);

		return PaymentMapper.toResponse(updated);
	}

	@Override
	@Transactional
	public PaymentResponse cancelPayment(Long paymentId, Long adminUserId) {
		log.info("Payment cancellation requested | paymentId={}, adminId={}", paymentId, adminUserId);

		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new PaymentNotFoundException(ErrorMessages.PAYMENT_NOT_FOUND + paymentId));

		if (payment.getStatus() == PaymentStatus.CANCELLED) {
			return PaymentMapper.toResponse(payment);
		}

		if (payment.getStatus() != PaymentStatus.PENDING) {
			throw new IllegalArgumentException(ErrorMessages.PAYMENT_CANCEL_INVALID_STATUS);
		}

		payment.setStatus(PaymentStatus.CANCELLED);
		payment.setUpdatedAt(LocalDateTime.now());

		Payment updated = paymentRepository.save(payment);
		return PaymentMapper.toResponse(updated);
	}

	// -----------------------------------------------------------------------
	// Admin: Revenue Summary
	// -----------------------------------------------------------------------

	@Override
	public AdminSummaryResponse getAdminSummary() {
		log.info(LogMessages.ADMIN_SUMMARY_FETCH);

		BigDecimal totalRevenue = paymentRepository.calculateTotalRevenue();
		BigDecimal totalRefunded = paymentRepository.calculateTotalRefunded();

		return AdminSummaryResponse.builder().totalRevenue(totalRevenue).totalRefunded(totalRefunded)
				.netRevenue(totalRevenue.subtract(totalRefunded)).totalTransactions(paymentRepository.count())
				.successfulTransactions(paymentRepository.countByStatus(PaymentStatus.SUCCESS))
				.failedTransactions(paymentRepository.countByStatus(PaymentStatus.FAILED))
				.refundedTransactions(paymentRepository.countByStatus(PaymentStatus.REFUNDED))
				.activeSubscriptions(
						paymentRepository.countByPaymentTypeAndStatus(PaymentType.SUBSCRIPTION, PaymentStatus.SUCCESS))
				.build();
	}

	// -----------------------------------------------------------------------
	// Private helpers
	// -----------------------------------------------------------------------

	private void verifyWebhookSignature(String payload, String signature) {
		try {
			boolean valid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
			if (!valid) {
				log.warn(LogMessages.WEBHOOK_SIGNATURE_INVALID);
				throw new WebhookVerificationException(ErrorMessages.INVALID_WEBHOOK_SIGNATURE);
			}
		} catch (RazorpayException e) {
			log.warn(LogMessages.WEBHOOK_SIGNATURE_INVALID);
			throw new WebhookVerificationException(ErrorMessages.INVALID_WEBHOOK_SIGNATURE);
		}
	}

	private void verifyCheckoutSignature(VerifyPaymentRequest request) {
		try {
			JSONObject attributes = new JSONObject();
			attributes.put("razorpay_order_id", request.getRazorpayOrderId());
			attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
			attributes.put("razorpay_signature", request.getRazorpaySignature());

			boolean valid = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
			if (!valid) {
				throw new WebhookVerificationException(ErrorMessages.INVALID_PAYMENT_SIGNATURE);
			}
		} catch (RazorpayException e) {
			throw new WebhookVerificationException(ErrorMessages.INVALID_PAYMENT_SIGNATURE);
		}
	}

	private void handlePaymentCaptured(String razorpayOrderId, String razorpayPaymentId) {
		Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElseThrow(
				() -> new PaymentNotFoundException(ErrorMessages.PAYMENT_NOT_FOUND_ORDER + razorpayOrderId));

		finalizeSuccessfulPayment(payment, razorpayPaymentId);
		log.info(LogMessages.WEBHOOK_PAYMENT_CAPTURED, razorpayOrderId, razorpayPaymentId);
	}

	private void handlePaymentFailed(String razorpayOrderId) {
		paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {
			if (payment.getStatus() == PaymentStatus.PENDING) {
				payment.setStatus(PaymentStatus.FAILED);
				payment.setUpdatedAt(LocalDateTime.now());
				paymentRepository.save(payment);
				log.info(LogMessages.WEBHOOK_PAYMENT_FAILED, razorpayOrderId);
			}
		});
	}

	private void finalizeSuccessfulPayment(Payment payment, String razorpayPaymentId) {
		if (payment.getStatus() == PaymentStatus.CANCELLED) {
			log.info("Payment is CANCELLED - skipping completion. orderId: {}", payment.getRazorpayOrderId());
			return;
		}

		if (payment.getStatus() == PaymentStatus.SUCCESS) {
			if (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isBlank()) {
				payment.setRazorpayPaymentId(razorpayPaymentId);
				payment.setUpdatedAt(LocalDateTime.now());
				paymentRepository.save(payment);
			}
			log.info("Payment already SUCCESS - skipping duplicate completion. orderId: {}", payment.getRazorpayOrderId());
			return;
		}

		payment.setStatus(PaymentStatus.SUCCESS);
		payment.setRazorpayPaymentId(razorpayPaymentId);
		payment.setUpdatedAt(LocalDateTime.now());
		paymentRepository.save(payment);
		publishSuccessEvent(payment);
	}

	private void publishSuccessEvent(Payment payment) {
		try {
			PaymentSuccessEvent event = PaymentSuccessEvent.builder().paymentId(payment.getPaymentId())
					.userId(payment.getUserId()).razorpayOrderId(payment.getRazorpayOrderId())
					.razorpayPaymentId(payment.getRazorpayPaymentId()).amount(payment.getAmount())
					.currency(payment.getCurrency().name()).paymentType(payment.getPaymentType()).build();

			rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, RabbitMQConfig.ROUTING_KEY_PAYMENT_SUCCESS,
					event);

			log.info(LogMessages.EVENT_PUBLISHED, "PaymentSuccess", payment.getUserId(), payment.getPaymentId());

		} catch (Exception e) {
			log.error(LogMessages.EVENT_PUBLISH_FAILED, payment.getPaymentId(), e.getMessage());
		}
	}

	private void publishRefundedEvent(Payment payment, BigDecimal refundedAmount) {
		try {
			PaymentRefundedEvent event = PaymentRefundedEvent.builder().paymentId(payment.getPaymentId())
					.userId(payment.getUserId()).razorpayOrderId(payment.getRazorpayOrderId())
					.razorpayRefundId(payment.getRazorpayRefundId()).refundedAmount(refundedAmount)
					.currency(payment.getCurrency().name()).refundNote(payment.getRefundNote()).build();

			rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, RabbitMQConfig.ROUTING_KEY_PAYMENT_REFUNDED,
					event);

			log.info(LogMessages.EVENT_PUBLISHED, "PaymentRefunded", payment.getUserId(), payment.getPaymentId());

		} catch (Exception e) {
			log.error(LogMessages.EVENT_PUBLISH_FAILED, payment.getPaymentId(), e.getMessage());
		}
	}

	private String buildIdempotencyKey(Long userId, CreateOrderRequest request) {
		return userId + ":" + request.getPaymentType().name() + ":" + request.getAmount().toPlainString() + ":"
				+ UUID.randomUUID().toString().substring(0, 8);
	}

	private String requireValidCheckoutKeyId() {
		String keyId = razorpayKeyId == null ? "" : razorpayKeyId.trim();
		if (!keyId.matches("^rzp_(test|live)_[A-Za-z0-9]+$")) {
			throw new IllegalStateException(ErrorMessages.RAZORPAY_KEY_ID_INVALID);
		}
		return keyId;
	}
}
