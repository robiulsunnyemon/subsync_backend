package com.rseelabs.subsync.modules.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rseelabs.subsync.modules.subscription.Subscription;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    @JsonProperty("fullName")
    private String fullName;
    @JsonProperty("totalMonthlyExpense")
    private BigDecimal totalMonthlyExpense;
    @JsonProperty("activeSubscriptionsCount")
    private int activeSubscriptionsCount;
    @JsonProperty("upcomingPayments")
    private List<Subscription> upcomingPayments;
    @JsonProperty("businessExpense")
    private BigDecimal businessExpense;
    @JsonProperty("personalExpense")
    private BigDecimal personalExpense;
    @JsonProperty("profileImage")
    private String profileImage;
}
