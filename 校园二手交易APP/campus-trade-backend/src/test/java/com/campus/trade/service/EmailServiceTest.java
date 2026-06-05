package com.campus.trade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.campus.trade.config.MailProperties;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.EmailTemplateType;
import com.campus.trade.model.enums.VerificationTokenType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailTemplateRenderer templateRenderer;

    private MailProperties mailProperties;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        mailProperties = new MailProperties();
        mailProperties.setEnabled(true);
        mailProperties.setFrom("noreply@test.com");
        mailProperties.setSignature("【测试签名】");
        mailProperties.setSupportEmail("support@test.com");
        mailProperties.setAppBaseUrl("https://test.app");
        mailProperties.setMaxAttempts(2);
        mailProperties.setRetryBackoffMillis(0);
        emailService = new EmailService(mailSender, mailProperties, templateRenderer);
    }

    @Test
    void sendVerificationCodeUsesTemplateAndMailSender() {
        when(templateRenderer.renderSubject(eq(EmailTemplateType.EMAIL_VERIFICATION), anyMap()))
                .thenReturn("验证码邮件");
        when(templateRenderer.renderBody(eq(EmailTemplateType.EMAIL_VERIFICATION), anyMap()))
                .thenReturn("body-content");

        User user = new User();
        user.setUsername("alice");
        user.setEmail("alice@test.com");

        emailService.sendVerificationCode(user, "123456", VerificationTokenType.EMAIL_VERIFICATION);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getSubject()).isEqualTo("验证码邮件");
        assertThat(message.getText()).contains("body-content");
    }

    @Test
    void sendTemplateEmailRetriesOnFailure() {
        when(templateRenderer.renderSubject(eq(EmailTemplateType.ORDER_STATUS_UPDATE), anyMap()))
                .thenReturn("订单状态更新");
        when(templateRenderer.renderBody(eq(EmailTemplateType.ORDER_STATUS_UPDATE), anyMap()))
                .thenReturn("content");

        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (attempts.getAndIncrement() == 0) {
                throw new MailSendException("fail");
            }
            return null;
        }).when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.sendTemplateEmail(
                "user@test.com",
                EmailTemplateType.ORDER_STATUS_UPDATE,
                Map.of("recipientName", "tester")));
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void skipPhysicalSendWhenDisabled() {
        mailProperties.setEnabled(false);
        when(templateRenderer.renderSubject(eq(EmailTemplateType.ORDER_STATUS_UPDATE), anyMap()))
                .thenReturn("subject");
        when(templateRenderer.renderBody(eq(EmailTemplateType.ORDER_STATUS_UPDATE), anyMap()))
                .thenReturn("body");

        emailService.sendTemplateEmail("user@test.com", EmailTemplateType.ORDER_STATUS_UPDATE, Map.of());

        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        verifyNoMoreInteractions(mailSender);
    }
}
