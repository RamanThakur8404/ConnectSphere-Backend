package com.connectsphere.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Revenue and transaction summary returned to admin dashboard.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSummaryResponse {

    private BigDecimal totalRevenue;
    private BigDecimal totalRefunded;
    private BigDecimal netRevenue;
    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private long refundedTransactions;
    private long activeSubscriptions;
}
