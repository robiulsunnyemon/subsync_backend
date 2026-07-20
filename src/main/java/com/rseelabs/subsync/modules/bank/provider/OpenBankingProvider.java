package com.rseelabs.subsync.modules.bank.provider;

import com.rseelabs.subsync.modules.bank.BankConnection;

import java.time.LocalDate;
import java.util.List;

public interface OpenBankingProvider {
    
    BankConnection.BankProvider getProviderType();

    String generateAuthLink(String redirectUri, String state, String market);

    String exchangeAuthorizationCode(String code);

    List<com.rseelabs.subsync.modules.bank.dto.BankProviderDto> fetchProviders(String countryCode);

    List<BankTransactionDTO> fetchTransactions(String accessToken, LocalDate from, LocalDate to);

    // DTO for returning transactions generically from any provider
    record BankTransactionDTO(
            String externalId,
            java.math.BigDecimal amount,
            String currency,
            LocalDate date,
            String merchantName,
            String description,
            String category
    ) {}
}
