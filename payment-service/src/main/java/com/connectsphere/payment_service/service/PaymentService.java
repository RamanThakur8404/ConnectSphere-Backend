package com.connectsphere.payment_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.connectsphere.payment_service.dto.AdminSummaryResponse;
import com.connectsphere.payment_service.dto.CreateOrderRequest;
import com.connectsphere.payment_service.dto.CreateOrderResponse;
import com.connectsphere.payment_service.dto.PaymentResponse;
import com.connectsphere.payment_service.dto.RefundRequest;
import com.connectsphere.payment_service.dto.SubscriptionStatusResponse;
import com.connectsphere.payment_service.dto.VerifyPaymentRequest;

// Service interface for payment operations.
public interface PaymentService {

    // Create order (PENDING state) 
    CreateOrderResponse createOrder(Long userId, CreateOrderRequest request);

    // User payment history 
    Page<PaymentResponse> getPaymentHistory(Long userId, Pageable pageable);

    // Admin payment history
    Page<PaymentResponse> getAllPayments(Pageable pageable);

    // Process Razorpay webhook 
    void handleWebhook(String payload, String signature);

    // Verify signed checkout success from the frontend and finalize the payment.
    PaymentResponse verifyPayment(Long userId, VerifyPaymentRequest request);

    // Get subscription status 
    SubscriptionStatusResponse getSubscriptionStatus(Long userId);

    // Process refund (ADMIN) 
    PaymentResponse processRefund(Long paymentId, RefundRequest request, Long adminUserId);

    // Approve pending payment after manual confirmation (ADMIN)
    PaymentResponse approvePayment(Long paymentId, Long adminUserId);

    // Cancel pending payment (ADMIN)
    PaymentResponse cancelPayment(Long paymentId, Long adminUserId);

    // Admin summary 
    AdminSummaryResponse getAdminSummary();
}
