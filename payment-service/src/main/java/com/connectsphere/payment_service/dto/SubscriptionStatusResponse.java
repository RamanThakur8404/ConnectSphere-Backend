package com.connectsphere.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Subscription status returned to the authenticated user.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatusResponse {

    private boolean active;
    private Long paymentId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime subscribedAt;

    // Estimated renewal date (subscribedAt + 30 days for monthly plans). 
    private LocalDateTime renewalDue;
}
