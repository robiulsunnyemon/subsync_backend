package com.rseelabs.subsync.modules.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankConnectionResponse {
    private UUID id;
    private String provider;
    private String institutionId;
    private String institutionName;
    private String accountId;
    private String status;
    private LocalDateTime connectedAt;
}
