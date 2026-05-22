package com.connectsphere.payment_service.repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.connectsphere.payment_service.constant.PaymentStatus;
import com.connectsphere.payment_service.constant.PaymentType;
import com.connectsphere.payment_service.entity.Payment;

// Repository for Payment entity.
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // User payment history (latest first) 
    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Find by Razorpay order ID 
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    // Check duplicate idempotency key 
    boolean existsByIdempotencyKey(String idempotencyKey);

    // Latest successful subscription 
    Optional<Payment> findTopByUserIdAndPaymentTypeAndStatusOrderByCreatedAtDesc(
            Long userId, PaymentType paymentType, PaymentStatus status);

    // Total revenue (SUCCESS) 
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal calculateTotalRevenue();

    // Total refunded 
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'REFUNDED'")
    BigDecimal calculateTotalRefunded();

    // Count by type and status 
    long countByPaymentTypeAndStatus(PaymentType paymentType, PaymentStatus status);

    // Count by status 
    long countByStatus(PaymentStatus status);
}