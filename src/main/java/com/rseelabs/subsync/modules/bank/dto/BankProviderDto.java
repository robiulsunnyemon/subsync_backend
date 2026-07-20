package com.rseelabs.subsync.modules.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankProviderDto {
    private String name;
    private String displayName;
    private String icon;
    private String type;
    private String market;
}
