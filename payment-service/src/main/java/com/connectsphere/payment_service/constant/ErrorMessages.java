package com.connectsphere.payment_service.constant;

// Centralized error messages for payment module.
public final class ErrorMessages {

    private ErrorMessages() {}

    // Payment creation
    public static final String AMOUNT_MUST_BE_POSITIVE = "Payment amount must be greater than zero";
    public static final String ORDER_CREATION_FAILED = "Failed to create payment order";
    public static final String RAZORPAY_ORDER_FAILED = "Razorpay order creation failed";
    public static final String RAZORPAY_KEY_ID_INVALID = "Razorpay key id is not configured correctly";
    public static final String DUPLICATE_REQUEST = "Duplicate payment request detected";
    public static final String DUPLICATE_IDEMPOTENCY_KEY =
            "Duplicate payment request detected in the recent retry window";

    // Payment lookup
    public static final String PAYMENT_NOT_FOUND = "Payment not found with id: ";
    public static final String PAYMENT_NOT_FOUND_ORDER = "Payment not found for order: ";
    public static final String UNAUTHORIZED_ACCESS = "Unauthorized payment access";

    // Webhook
    public static final String INVALID_SIGNATURE = "Invalid webhook signature";
    public static final String INVALID_WEBHOOK_SIGNATURE = INVALID_SIGNATURE;
    public static final String INVALID_PAYMENT_SIGNATURE = "Invalid payment signature";
    public static final String WEBHOOK_FAILED = "Webhook processing failed";

    // Refund
    public static final String INVALID_REFUND_STATUS = "Only successful payments can be refunded";
    public static final String REFUND_NOT_SUCCESS_STATUS = INVALID_REFUND_STATUS;
    public static final String REFUND_FAILED = "Refund processing failed";
    public static final String RAZORPAY_REFUND_FAILED = "Razorpay refund failed";
    public static final String REFUND_EXCEEDS_AMOUNT = "Refund exceeds original amount";
    public static final String REFUND_AMOUNT_EXCEEDS = REFUND_EXCEEDS_AMOUNT;
    public static final String REFUND_PAYMENT_ID_REQUIRED = "Refund requires a captured Razorpay payment id";
    public static final String PAYMENT_APPROVAL_INVALID_STATUS = "Only pending payments can be approved";
    public static final String PAYMENT_CANCEL_INVALID_STATUS = "Only pending payments can be cancelled";

    // Subscription
    public static final String NO_ACTIVE_SUBSCRIPTION = "No active subscription found";

    // Authorization
    public static final String ADMIN_REQUIRED = "Admin access required";
    
    
    
}
