package com.rseelabs.subsync.modules.notification;

import com.rseelabs.subsync.modules.subscription.Subscription;
import com.rseelabs.subsync.modules.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationSenderImpl implements NotificationSender {

    private final JavaMailSender javaMailSender;

    @Override
    public String getType() {
        return "EMAIL";
    }

    @Override
    public void sendRenewalReminder(User user, Subscription subscription, int daysRemaining) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("Cannot send email to user {} because email address is missing", user.getId());
            return;
        }

        String subject = String.format("SubSync Reminder: %s renewing in %d days", 
                subscription.getMerchantName(), daysRemaining);
                
        String text = String.format(
                "Hello %s,\n\n" +
                "This is a quick reminder from SubSync that your subscription for %s is set to renew on %s.\n\n" +
                "Amount: %s %s\n\n" +
                "If you wish to cancel this subscription, please make sure to do it before the renewal date.\n\n" +
                "Best regards,\n" +
                "The SubSync Team",
                user.getFullName() != null ? user.getFullName() : "User",
                subscription.getMerchantName(),
                subscription.getNextBillingDate().toString(),
                subscription.getAmount().toPlainString(),
                subscription.getCurrency()
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(text);

        try {
            javaMailSender.send(message);
            log.info("Sent email reminder to {} for subscription {}", user.getEmail(), subscription.getId());
        } catch (Exception e) {
            log.error("Failed to send email reminder to {}", user.getEmail(), e);
        }
    }
}
