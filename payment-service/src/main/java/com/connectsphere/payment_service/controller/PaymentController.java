package com.connectsphere.payment_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.payment_service.dto.AdminSummaryResponse;
import com.connectsphere.payment_service.dto.CreateOrderRequest;
import com.connectsphere.payment_service.dto.CreateOrderResponse;
import com.connectsphere.payment_service.dto.PaymentResponse;
import com.connectsphere.payment_service.dto.RefundRequest;
import com.connectsphere.payment_service.dto.SubscriptionStatusResponse;
import com.connectsphere.payment_service.dto.ApiResponse;
import com.connectsphere.payment_service.dto.VerifyPaymentRequest;
import com.connectsphere.payment_service.service.PaymentService;

import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// REST controller for payment operations.
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment operations")
public class PaymentController {

	private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

	private final PaymentService paymentService;

	// USER APIs

	// Create payment order 
	@PostMapping("/order")
	public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(Authentication authentication,
			@Valid @RequestBody CreateOrderRequest request) {

		Long userId = extractUserId(authentication);
		log.info("Create order | userId={}, type={}", userId, request.getPaymentType());

		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Order created successfully", paymentService.createOrder(userId, request)));
	}

	// Get user payment history 
	@GetMapping("/history")
	public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPaymentHistory(Authentication authentication,
			@PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

		Long userId = extractUserId(authentication);
		log.info("Fetch history | userId={}, page={}", userId, pageable.getPageNumber());

		return ResponseEntity.ok(ApiResponse.success("Payment history retrieved", paymentService.getPaymentHistory(userId, pageable)));
	}

	// Get subscription status 
	@GetMapping("/subscription/status")
	public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> getSubscriptionStatus(Authentication authentication) {

		return ResponseEntity.ok(ApiResponse.success("Subscription status retrieved", paymentService.getSubscriptionStatus(extractUserId(authentication))));
	}

	// Verify signed Razorpay checkout success directly from the frontend.
	@PostMapping("/verify")
	public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(Authentication authentication,
			@Valid @RequestBody VerifyPaymentRequest request) {

		Long userId = extractUserId(authentication);
		log.info("Verify payment | userId={}, orderId={}", userId, request.getRazorpayOrderId());

		return ResponseEntity.ok(ApiResponse.success("Payment verified successfully",
				paymentService.verifyPayment(userId, request)));
	}

	// WEBHOOK (public, signature-secured)

	// Handle Razorpay webhook 
	@PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> handleWebhook(@RequestBody String payload,
			@RequestHeader("Razorpay-Signature") String signature) {

		log.info("Webhook received | signaturePresent={}", signature != null);
		paymentService.handleWebhook(payload, signature);

		return ResponseEntity.ok("Webhook processed successfully"); // Webhooks usually expect raw 200 OK string, so not wrapping this in ApiResponse
	}

	// ADMIN APIs

	// Get all payment transactions (ADMIN)
	@GetMapping("/admin/history")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

		log.info("Fetch all payment history | page={}", pageable.getPageNumber());
		return ResponseEntity.ok(ApiResponse.success("All payments retrieved", paymentService.getAllPayments(pageable)));
	}

	// Process refund (ADMIN) 
	@PostMapping("/admin/refund/{paymentId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<PaymentResponse>> processRefund(Authentication authentication, @PathVariable Long paymentId,
			@Valid @RequestBody RefundRequest request) {

		Long adminId = extractUserId(authentication);
		log.info("Refund request | paymentId={}, adminId={}", paymentId, adminId);

		return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", paymentService.processRefund(paymentId, request, adminId)));
	}

	// Approve pending payment after manual payment confirmation (ADMIN)
	@PostMapping("/admin/approve/{paymentId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<PaymentResponse>> approvePayment(Authentication authentication, @PathVariable Long paymentId) {

		Long adminId = extractUserId(authentication);
		log.info("Approve payment | paymentId={}, adminId={}", paymentId, adminId);

		return ResponseEntity.ok(ApiResponse.success("Payment approved successfully", paymentService.approvePayment(paymentId, adminId)));
	}

	// Cancel pending payment (ADMIN)
	@PostMapping("/admin/cancel/{paymentId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(Authentication authentication, @PathVariable Long paymentId) {

		Long adminId = extractUserId(authentication);
		log.info("Cancel payment | paymentId={}, adminId={}", paymentId, adminId);

		return ResponseEntity.ok(ApiResponse.success("Payment cancelled successfully", paymentService.cancelPayment(paymentId, adminId)));
	}

	// Admin summary 
	@GetMapping("/admin/summary")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<AdminSummaryResponse>> getAdminSummary() {
		log.info("Fetch admin summary");
		return ResponseEntity.ok(ApiResponse.success("Admin summary retrieved", paymentService.getAdminSummary()));
	}

	// Extract userId from security context 
	private Long extractUserId(Authentication authentication) {
		return (Long) authentication.getPrincipal();
	}
}
