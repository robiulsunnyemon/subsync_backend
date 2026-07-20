package com.rseelabs.subsync.modules.notification;

import com.rseelabs.subsync.modules.subscription.Subscription;
import com.rseelabs.subsync.modules.user.User;

public interface NotificationSender {
    /**
     * @return the type of notification this sender handles (e.g., "EMAIL", "PUSH")
     */
    String getType();

    /**
     * Sends a reminder notification to the user about an upcoming subscription renewal.
     * 
     * @param user The user to notify
     * @param subscription The subscription that is renewing
     * @param daysRemaining Days until the renewal
     */
    void sendRenewalReminder(User user, Subscription subscription, int daysRemaining);
}
