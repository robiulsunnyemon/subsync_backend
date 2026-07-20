package com.rseelabs.subsync.modules.bank.provider;

import com.rseelabs.subsync.modules.bank.BankConnection;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BankProviderFactory {

    private final Map<BankConnection.BankProvider, OpenBankingProvider> providers;

    public BankProviderFactory(List<OpenBankingProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(OpenBankingProvider::getProviderType, Function.identity()));
    }

    public OpenBankingProvider getProvider(BankConnection.BankProvider type) {
        OpenBankingProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported Bank Provider: " + type);
        }
        return provider;
    }
}
