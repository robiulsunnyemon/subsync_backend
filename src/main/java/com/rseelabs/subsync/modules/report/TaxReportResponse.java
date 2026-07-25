package com.rseelabs.subsync.modules.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class TaxReportResponse {
    private int year;
    private BigDecimal totalDeductions;
    private List<Map<String, Object>> subscriptions;
}
