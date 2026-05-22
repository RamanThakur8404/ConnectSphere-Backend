package com.connectsphere.payment_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.connectsphere.payment_service.constant.Currency;
import com.connectsphere.payment_service.constant.PaymentStatus;
import com.connectsphere.payment_service.constant.PaymentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Immutable payment entity.
@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payment_user_id",    columnList = "userId"),
        @Index(name = "idx_payment_order_id",   columnList = "razorpayOrderId"),
        @Index(name = "idx_payment_status",     columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    // Auto-generated primary key. 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    // Logical FK to cs_auth.users.userId.
    @Column(nullable = false)
    private Long userId;

    // Razorpay Order ID (e.g., order_XXXXXXXXXX).
    @Column(nullable = false, unique = true, length = 100)
    private String razorpayOrderId;

    // Razorpay Payment ID (e.g., pay_XXXXXXXXXX).
    @Column(length = 100)
    private String razorpayPaymentId;

    // Idempotency key sent to Razorpay to prevent duplicate charges.
    @Column(unique = true, length = 100)
    private String idempotencyKey;

    // Amount in the smallest currency unit (paise for INR).
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // ISO 4217 currency code (INR, USD, etc.).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Currency currency = Currency.INR;

    // Category of this payment transaction.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentType paymentType;

    // Current status. Set to PENDING on creation.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // Human-readable description forwarded to Razorpay receipt field.
    @Column(length = 255)
    private String description;

    // Admin note on refund (populated when status → REFUNDED).
    @Column(length = 500)
    private String refundNote;

    // Razorpay Refund ID, populated after a successful refund API call.
    @Column(length = 100)
    private String razorpayRefundId;

    // Timestamp of transaction initiation. 
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Timestamp when webhook updated the status. 
    private LocalDateTime updatedAt;

    @PrePersist
    public void beforeSave() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
        if (this.currency == null) {
            this.currency = Currency.INR;
        }
    }
}
