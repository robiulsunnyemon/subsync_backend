package com.rseelabs.subsync.modules.dashboard;

import com.rseelabs.subsync.modules.subscription.Subscription;
import com.rseelabs.subsync.modules.subscription.SubscriptionEngine;
import com.rseelabs.subsync.modules.subscription.SubscriptionRepository;
import com.rseelabs.subsync.modules.user.User;
import com.rseelabs.subsync.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.rseelabs.subsync.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionEngine subscriptionEngine;

    public DashboardResponse getDashboardSummary(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Subscription> activeSubscriptions = subscriptionRepository.findAllByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE);
        log.info("Dashboard loaded for user: fullName='{}', email='{}'", user.getFullName(), user.getEmail());
        
        // Re-categorize any UNCATEGORIZED subscriptions on-the-fly
        List<Subscription> uncategorized = activeSubscriptions.stream()
                .filter(s -> s.getType() == Subscription.ExpenseType.UNCATEGORIZED)
                .collect(Collectors.toList());
        if (!uncategorized.isEmpty()) {
            uncategorized.forEach(s -> s.setType(subscriptionEngine.predictExpenseType(s.getMerchantName())));
            subscriptionRepository.saveAll(uncategorized);
            // Reload updated list
            activeSubscriptions = subscriptionRepository.findAllByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE);
        }
        
        BigDecimal totalExpense = activeSubscriptions.stream()
                .map(Subscription::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal businessExpense = activeSubscriptions.stream()
                .filter(s -> s.getType() == Subscription.ExpenseType.BUSINESS)
                .map(Subscription::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal personalExpense = activeSubscriptions.stream()
                .filter(s -> s.getType() == Subscription.ExpenseType.PERSONAL)
                .map(Subscription::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        LocalDate today = LocalDate.now();
        LocalDate plus5Days = today.plusDays(5);
        
        List<Subscription> topUpcoming = activeSubscriptions.stream()
                .filter(s -> !s.getNextBillingDate().isBefore(today) && !s.getNextBillingDate().isAfter(plus5Days))
                .sorted((s1, s2) -> s1.getNextBillingDate().compareTo(s2.getNextBillingDate()))
                .collect(Collectors.toList());
                
        if (topUpcoming.isEmpty()) {
            topUpcoming = activeSubscriptions.stream()
                    .filter(s -> !s.getNextBillingDate().isBefore(today))
                    .sorted((s1, s2) -> s1.getNextBillingDate().compareTo(s2.getNextBillingDate()))
                    .limit(5)
                    .collect(Collectors.toList());
        }

        return DashboardResponse.builder()
                .fullName(user.getFullName())
                .profileImage(user.getProfileImage())
                .totalMonthlyExpense(totalExpense)
                .activeSubscriptionsCount(activeSubscriptions.size())
                .upcomingPayments(topUpcoming)
                .businessExpense(businessExpense)
                .personalExpense(personalExpense)
                .build();
    }
}
