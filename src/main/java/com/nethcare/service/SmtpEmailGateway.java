package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "nethcare.email.provider", havingValue = "smtp")
public class SmtpEmailGateway implements EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailGateway.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailGateway(JavaMailSender mailSender,
                            @Value("${nethcare.email.smtp.from:no-reply@nethcare.lk}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public String send(String destination, String subject, String message) {
        if (destination == null || destination.isBlank()) {
            throw new BusinessException("Recipient email address is required.");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(destination.trim());
            helper.setSubject(subject != null && !subject.isBlank() ? subject.trim() : "Nethcare Notification");
            helper.setText(message, false);

            mailSender.send(mimeMessage);

            String receiptId = "SMTP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
            log.info("Sent email to {} via SMTP (receipt={})", destination, receiptId);
            return receiptId;
        } catch (Exception ex) {
            log.error("Failed to send email to {} via SMTP: {}", destination, ex.getMessage(), ex);
            throw new BusinessException("SMTP dispatch failed: " + ex.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "Live SMTP (" + fromAddress + ")";
    }

    @Override
    public boolean isSimulator() {
        return false;
    }
}
