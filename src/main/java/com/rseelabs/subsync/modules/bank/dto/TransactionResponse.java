package com.rseelabs.subsync.modules.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private UUID id;
    private String externalTransactionId;
    private BigDecimal amount;
    private String currency;
    private LocalDate date;
    private String merchantName;
    private String category;
    private String description;
    private String bankName;
}
