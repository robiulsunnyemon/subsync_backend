package com.rseelabs.subsync.modules.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class TaxReportResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDeductions;
    private List<Map<String, Object>> subscriptions;
}
