package com.rseelabs.subsync.modules.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCleanupService {

    private final UserRepository userRepository;

    // Run every hour at minute 0
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupUnverifiedExpiredUsers() {
        log.info("Running scheduled cleanup for unverified users with expired OTPs...");
        try {
            userRepository.deleteAllByIsEmailVerifiedFalseAndOtpExpiryBefore(LocalDateTime.now());
            log.info("Successfully cleaned up expired unverified users.");
        } catch (Exception e) {
            log.error("Error during cleanup of unverified users: ", e);
        }
    }
}
