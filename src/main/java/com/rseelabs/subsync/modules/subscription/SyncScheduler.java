package com.rseelabs.subsync.modules.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SyncScheduler {

    private final SubscriptionEngine subscriptionEngine;

    // Run every day at 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void runDailyBankSync() {
        log.info("Starting daily bank sync to detect new subscriptions...");
        
        // In a complete implementation, this would:
        // 1. Fetch all active BankConnections from DB
        // 2. Loop through them and get their associated OpenBankingProvider using BankProviderFactory
        // 3. Fetch transactions from the provider since the last sync date
        // 4. Save new transactions to the DB
        // 5. Run subscriptionEngine.detectSubscriptions on the new transactions
        // 6. Save newly detected subscriptions to the DB
        
        log.info("Daily bank sync completed.");
    }
}
