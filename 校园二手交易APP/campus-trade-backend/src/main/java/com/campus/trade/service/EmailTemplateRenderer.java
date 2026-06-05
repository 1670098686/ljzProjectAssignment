package com.campus.trade.service;

import com.campus.trade.model.enums.EmailTemplateType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EmailTemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateRenderer.class);
    private final Map<EmailTemplateType, String> templateCache = new ConcurrentHashMap<>();

    public String renderBody(EmailTemplateType templateType, Map<String, Object> variables) {
        String template = templateCache.computeIfAbsent(templateType, this::loadTemplateContent);
        return replacePlaceholders(template, variables);
    }

    public String renderSubject(EmailTemplateType templateType, Map<String, Object> variables) {
        return replacePlaceholders(templateType.getSubjectTemplate(), variables);
    }

    private String loadTemplateContent(EmailTemplateType templateType) {
        ClassPathResource resource = new ClassPathResource("templates/" + templateType.getTemplatePath());
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Failed to load email template: {}", templateType.getTemplatePath(), ex);
            throw new IllegalStateException("无法加载邮件模板: " + templateType.name(), ex);
        }
    }

    private String replacePlaceholders(String template, Map<String, Object> variables) {
        if (!StringUtils.hasText(template)) {
            return "";
        }
        String resolved = template;
        Map<String, Object> safeVariables = variables == null ? new HashMap<>() : new HashMap<>(variables);
        for (Map.Entry<String, Object> entry : safeVariables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            resolved = resolved.replace(placeholder, entry.getValue() == null ? "" : entry.getValue().toString());
        }
        return resolved;
    }
}
