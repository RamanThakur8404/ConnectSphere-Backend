package com.connectsphere.payment_service.constant;

// Centralized log message templates for payment module.
public final class LogMessages {

    public static final String ORDER_CREATE_ATTEMPT = "Creating order | userId={}, amount={}, type={}";

	private LogMessages() {}

    // Order
    public static final String ORDER_CREATE_START   = "Creating order | userId={}, amount={}, type={}";
    public static final String ORDER_CREATE_SUCCESS = "Order created | orderId={}, paymentId={}";
    public static final String ORDER_CREATE_ERROR   = "Order creation failed | userId={}, error={}";

    // Webhook
    public static final String WEBHOOK_RECEIVED     = "Webhook received | event={}";
    public static final String WEBHOOK_VALID        = "Webhook signature valid";
    public static final String WEBHOOK_INVALID      = "Webhook signature invalid";
    public static final String PAYMENT_CAPTURED     = "Payment captured | orderId={}, paymentId={}";
    public static final String PAYMENT_FAILED       = "Payment failed | orderId={}";
    public static final String WEBHOOK_IGNORED      = "Webhook ignored | event={}";

    // History
    public static final String HISTORY_FETCH        = "Fetching history | userId={}, page={}";

    // Refund
    public static final String REFUND_START         = "Refund initiated | paymentId={}, adminId={}";
    public static final String REFUND_SUCCESS       = "Refund successful | paymentId={}, refundId={}";
    public static final String REFUND_ERROR         = "Refund failed | paymentId={}, error={}";

    // Subscription
    public static final String SUBSCRIPTION_FETCH   = "Fetching subscription | userId={}";

    // Events
    public static final String EVENT_PUBLISHED      = "Event published | type={}, userId={}, paymentId={}";
    public static final String EVENT_ERROR          = "Event publish failed | paymentId={}, error={}";

    // Admin
    public static final String ADMIN_SUMMARY_FETCH  = "Fetching admin summary";

    // Aliases used by PaymentServiceImpl
    public static final String ORDER_CREATE_FAILED         = ORDER_CREATE_ERROR;
    public static final String WEBHOOK_SIGNATURE_VALID     = WEBHOOK_VALID;
    public static final String WEBHOOK_SIGNATURE_INVALID   = WEBHOOK_INVALID;
    public static final String WEBHOOK_EVENT_IGNORED       = WEBHOOK_IGNORED;
    public static final String SUBSCRIPTION_STATUS_FETCH   = SUBSCRIPTION_FETCH;
    public static final String REFUND_ATTEMPT              = REFUND_START;
    public static final String REFUND_FAILED               = REFUND_ERROR;
    public static final String WEBHOOK_PAYMENT_CAPTURED    = PAYMENT_CAPTURED;
    public static final String WEBHOOK_PAYMENT_FAILED      = PAYMENT_FAILED;
    public static final String EVENT_PUBLISH_FAILED        = EVENT_ERROR;
}