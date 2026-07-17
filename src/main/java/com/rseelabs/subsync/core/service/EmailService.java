package com.rseelabs.subsync.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@subsync.com");

            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.warn("Failed to send email to {} due to incorrect credentials. Email body: \n{}", to, body);
        }
    }
    
    public void sendOtpEmail(String to, String otp) {
        String subject = "SubSync - Email Verification OTP";
        String body = "Your OTP for verifying your email address is: " + otp + 
                      "\nThis OTP is valid for 10 minutes.";
        log.info("=============================================");
        log.info("OTP FOR {}: {}", to, otp);
        log.info("=============================================");
        sendEmail(to, subject, body);
    }
}
