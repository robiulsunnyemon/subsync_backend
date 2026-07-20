package com.rseelabs.subsync.modules.dashboard;

import com.rseelabs.subsync.modules.subscription.Subscription;
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

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public DashboardResponse getDashboardSummary(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Subscription> activeSubscriptions = subscriptionRepository.findAllByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE);
        
        BigDecimal totalExpense = activeSubscriptions.stream()
                .map(Subscription::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add); // Assuming monthly normalization for simplicity in this MVP
                
        LocalDate today = LocalDate.now();
        List<Subscription> upcoming = subscriptionRepository.findUpcomingSubscriptions(user, today, today.plusDays(30));
        
        // Only return top 5 upcoming
        List<Subscription> topUpcoming = upcoming.stream()
                .sorted((s1, s2) -> s1.getNextBillingDate().compareTo(s2.getNextBillingDate()))
                .limit(5)
                .collect(Collectors.toList());

        return DashboardResponse.builder()
                .totalMonthlyExpense(totalExpense)
                .activeSubscriptionsCount(activeSubscriptions.size())
                .upcomingPayments(topUpcoming)
                .build();
    }
}
