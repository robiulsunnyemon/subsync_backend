package com.rseelabs.subsync.modules.notification;

import com.rseelabs.subsync.modules.subscription.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class NotificationEngine {

    private final List<NotificationSender> senders;

    // Run every day at 8 AM
    @Scheduled(cron = "0 0 8 * * ?")
    public void runDailyReminders() {
        log.info("Starting daily notification engine for subscription reminders...");

        // Map senders by their type to easily select them
        Map<String, NotificationSender> senderMap = senders.stream()
                .collect(Collectors.toMap(NotificationSender::getType, Function.identity()));

        // In a complete implementation:
        // 1. Query the DB for all active Subscriptions
        // 2. Loop through them and fetch the User's NotificationSettings
        // 3. Calculate days until nextBillingDate:
        //    long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), sub.getNextBillingDate());
        // 4. Check if daysRemaining matches the user's setting (e.g., reminderDaysBefore == 3 and daysRemaining == 3)
        // 5. If it matches, check which channels are enabled (emailEnabled, pushEnabled)
        // 6. Use the appropriate NotificationSender from senderMap to send the reminder.
        
        // Example:
        // if (settings.isEmailEnabled() && senderMap.containsKey("EMAIL")) {
        //     senderMap.get("EMAIL").sendRenewalReminder(user, sub, (int) daysRemaining);
        // }

        log.info("Daily notification engine completed.");
    }
}
