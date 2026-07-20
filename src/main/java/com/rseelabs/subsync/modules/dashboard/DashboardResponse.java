package com.rseelabs.subsync.modules.dashboard;

import com.rseelabs.subsync.modules.subscription.Subscription;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private BigDecimal totalMonthlyExpense;
    private int activeSubscriptionsCount;
    private List<Subscription> upcomingPayments;
}
