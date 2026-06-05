package com.campus.trade.service;

import com.campus.trade.config.MailProperties;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.EmailTemplateType;
import com.campus.trade.model.enums.VerificationTokenType;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final int MOCK_PREVIEW_LIMIT = 120;

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final EmailTemplateRenderer templateRenderer;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender, 
                       MailProperties mailProperties, 
                       EmailTemplateRenderer templateRenderer) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.templateRenderer = templateRenderer;
    }

    public void sendVerificationCode(User user, String code, VerificationTokenType type) {
        String email;
        String username;
        if (user != null) {
            email = user.getEmail();
            username = user.getUsername();
        } else {
            // 如果user为null，无法发送邮件
            log.warn("Skip sending verification email because user is null");
            return;
        }
        
        if (!StringUtils.hasText(email)) {
            log.warn("Skip sending verification email because user email is empty");
            return;
        }
        
        EmailTemplateType templateType = type == VerificationTokenType.EMAIL_VERIFICATION
                ? EmailTemplateType.EMAIL_VERIFICATION
                : EmailTemplateType.PASSWORD_RESET;
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username != null ? username : "用户");
        variables.put("recipientName", username != null ? username : "用户");
        variables.put("code", code);
        variables.put("validMinutes", 30);
        variables.put("actionUrl", mailProperties.getAppBaseUrl());
        sendTemplateEmail(email, templateType, variables);
    }

    public void sendTemplateEmail(String recipient, EmailTemplateType templateType, Map<String, Object> variables) {
        if (!StringUtils.hasText(recipient)) {
            log.warn("Skip sending email for template {} because recipient is empty", templateType);
            return;
        }
        Map<String, Object> resolvedVariables = prepareVariables(variables);
        String subject = templateRenderer.renderSubject(templateType, resolvedVariables);
        String body = templateRenderer.renderBody(templateType, resolvedVariables);
        dispatchEmail(recipient, subject, body);
    }

    private Map<String, Object> prepareVariables(Map<String, Object> customVariables) {
        Map<String, Object> variables = new HashMap<>();
        if (customVariables != null) {
            variables.putAll(customVariables);
        }
        variables.putIfAbsent("signature", mailProperties.getSignature());
        variables.putIfAbsent("supportEmail", mailProperties.getSupportEmail());
        variables.putIfAbsent("appBaseUrl", mailProperties.getAppBaseUrl());
        return variables;
    }

    private void dispatchEmail(String to, String subject, String content) {
        if (!mailProperties.isEnabled() || mailSender == null) {
            log.info("[MockMail] to='{}', subject='{}', preview='{}' (mail.enabled={}, mailSender={})", 
                     to, subject, preview(content), mailProperties.isEnabled(), mailSender == null ? "null" : "configured");
            return;
        }

        int attempt = 0;
        int maxAttempts = Math.max(1, mailProperties.getMaxAttempts());
        while (attempt < maxAttempts) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                if (StringUtils.hasText(mailProperties.getFrom())) {
                    message.setFrom(mailProperties.getFrom());
                }
                message.setTo(to);
                message.setSubject(subject);
                message.setText(content);
                mailSender.send(message);
                log.info("Sent email '{}' to {} (attempt {}/{})", subject, to, attempt + 1, maxAttempts);
                return;
            } catch (Exception ex) {
                attempt++;
                if (attempt >= maxAttempts) {
                    log.error("Failed to send email '{}' to {} after {} attempts", subject, to, attempt, ex);
                    return;
                }
                long backoff = Math.max(0, mailProperties.getRetryBackoffMillis());
                if (backoff > 0) {
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        log.warn("Mail sending retry interrupted for subject '{}'", subject);
                        return;
                    }
                }
            }
        }
    }

    private String preview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replaceAll("\n", " ").trim();
        if (normalized.length() <= MOCK_PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, MOCK_PREVIEW_LIMIT) + "...";
    }
}
