package com.rseelabs.subsync.modules.subscription;

import com.rseelabs.subsync.modules.bank.BankConnection;
import com.rseelabs.subsync.modules.bank.BankConnectionRepository;
import com.rseelabs.subsync.modules.bank.Transaction;
import com.rseelabs.subsync.modules.bank.TransactionRepository;
import com.rseelabs.subsync.modules.bank.provider.BankProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SyncScheduler {

    private final SubscriptionEngine subscriptionEngine;
    private final BankConnectionRepository bankConnectionRepository;
    private final TransactionRepository transactionRepository;
    private final BankProviderFactory providerFactory;
    private final SubscriptionRepository subscriptionRepository;

    // Run every day at 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void runDailyBankSync() {
        log.info("Starting daily bank sync to detect new subscriptions...");
        
        try {
            // 1. Fetch all active BankConnections from DB
            List<BankConnection> activeConnections = bankConnectionRepository.findByStatus(BankConnection.ConnectionStatus.CONNECTED);
            
            for (BankConnection connection : activeConnections) {
                log.info("Syncing connection: {} - ID: {}", connection.getInstitutionName(), connection.getId());
                
                // 2. Get OpenBankingProvider using BankProviderFactory
                var provider = providerFactory.getProvider(connection.getProvider());
                
                // 3. Fetch transactions (sync last 30 days)
                java.time.LocalDate to = java.time.LocalDate.now();
                java.time.LocalDate from = to.minusDays(30);
                
                String accessToken = connection.getConsentId();
                if (accessToken == null || accessToken.isEmpty()) {
                    log.warn("Connection {} has no consent token, skipping sync.", connection.getId());
                    continue;
                }
                
                var dtos = provider.fetchTransactions(accessToken, from, to);
                
                // 4. Save new transactions to the DB
                for (var dto : dtos) {
                    if (!transactionRepository.existsByExternalTransactionIdAndBankConnection(dto.externalId(), connection)) {
                        Transaction tx = Transaction.builder()
                                .bankConnection(connection)
                                .externalTransactionId(dto.externalId())
                                .amount(dto.amount())
                                .currency(dto.currency() != null ? dto.currency() : "EUR")
                                .date(dto.date())
                                .merchantName(dto.merchantName())
                                .category(dto.category())
                                .description(dto.description())
                                .build();
                        transactionRepository.save(tx);
                    }
                }
                
                // 5. Run subscriptionEngine.detectSubscriptions on the connection's transactions
                List<Transaction> userTransactions = transactionRepository.findByBankConnection(connection);
                List<Subscription> detected = subscriptionEngine.detectSubscriptions(connection.getUser(), userTransactions);
                
                // 6. Save newly detected subscriptions to the DB
                for (Subscription sub : detected) {
                    boolean exists = subscriptionRepository.findAllByUser(connection.getUser()).stream()
                            .anyMatch(s -> s.getMerchantName().equalsIgnoreCase(sub.getMerchantName()));
                    if (!exists) {
                        subscriptionRepository.save(sub);
                        log.info("Daily Sync: Detected and saved new subscription {} for user {}", sub.getMerchantName(), connection.getUser().getEmail());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during daily bank sync: {}", e.getMessage(), e);
        }
        
        log.info("Daily bank sync completed.");
    }
}
