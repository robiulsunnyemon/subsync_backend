package com.rseelabs.subsync.modules.subscription;

import com.rseelabs.subsync.modules.bank.Transaction;
import com.rseelabs.subsync.modules.user.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SubscriptionEngine {

    /**
     * Analyzes a list of recent transactions and identifies new subscriptions.
     * Returns a list of newly detected Subscriptions.
     */
    public List<Subscription> detectSubscriptions(User user, List<Transaction> recentTransactions) {
        List<Subscription> detectedSubscriptions = new ArrayList<>();

        // Group transactions by merchant name
        Map<String, List<Transaction>> groupedByMerchant = recentTransactions.stream()
                .filter(t -> t.getMerchantName() != null && !t.getMerchantName().isEmpty())
                .collect(Collectors.groupingBy(Transaction::getMerchantName));

        for (Map.Entry<String, List<Transaction>> entry : groupedByMerchant.entrySet()) {
            String merchantName = entry.getKey();
            List<Transaction> transactions = entry.getValue();

            // Needs at least 2 transactions to detect a pattern
            if (transactions.size() < 2) continue;

            // Sort by date (descending to get the most recent first)
            transactions.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));

            Transaction latest = transactions.get(0);
            Transaction previous = transactions.get(1);

            // Check if amounts are exactly the same (simple logic for MVP)
            if (latest.getAmount().compareTo(previous.getAmount()) == 0) {
                
                long daysBetween = Math.abs(ChronoUnit.DAYS.between(latest.getDate(), previous.getDate()));
                
                Subscription.BillingCycle cycle = null;
                
                // Identify cycle based on day diff
                if (daysBetween >= 25 && daysBetween <= 35) {
                    cycle = Subscription.BillingCycle.MONTHLY;
                } else if (daysBetween >= 6 && daysBetween <= 8) {
                    cycle = Subscription.BillingCycle.WEEKLY;
                } else if (daysBetween >= 350 && daysBetween <= 380) {
                    cycle = Subscription.BillingCycle.YEARLY;
                }

                if (cycle != null) {
                    // Calculate next billing date
                    LocalDate nextBilling = calculateNextBillingDate(latest.getDate(), cycle);
                    
                    Subscription newSub = Subscription.builder()
                            .user(user)
                            .merchantName(merchantName)
                            .amount(latest.getAmount().abs()) // store as positive
                            .currency(latest.getCurrency())
                            .cycle(cycle)
                            .nextBillingDate(nextBilling)
                            .type(Subscription.ExpenseType.UNCATEGORIZED)
                            .status(Subscription.SubscriptionStatus.ACTIVE)
                            .build();
                            
                    detectedSubscriptions.add(newSub);
                }
            }
        }
        
        return detectedSubscriptions;
    }

    private LocalDate calculateNextBillingDate(LocalDate lastPayment, Subscription.BillingCycle cycle) {
        return switch (cycle) {
            case WEEKLY -> lastPayment.plusWeeks(1);
            case MONTHLY -> lastPayment.plusMonths(1);
            case YEARLY -> lastPayment.plusYears(1);
        };
    }
}
