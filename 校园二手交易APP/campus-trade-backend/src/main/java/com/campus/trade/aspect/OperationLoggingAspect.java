package com.campus.trade.aspect;

import com.campus.trade.annotation.OperationLog;
import com.campus.trade.model.entity.OperationLogEntry;
import com.campus.trade.model.enums.OperationResult;
import com.campus.trade.repository.OperationLogRepository;
import com.campus.trade.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Aspect
@Component
public class OperationLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLoggingAspect.class);
    private static final int MAX_FIELD_LENGTH = 2048;
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile("\\\"(password|pwd|token|secret|code)\\\"\\s*:\\s*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern SENSITIVE_QUERY_PATTERN = Pattern.compile("(?i)(password|pwd|token|secret|code)=([^&]*)");

    private final OperationLogRepository logRepository;
    private final ObjectMapper objectMapper;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public OperationLoggingAspect(OperationLogRepository logRepository, ObjectMapper objectMapper) {
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
    }

    @Pointcut("@annotation(com.campus.trade.annotation.OperationLog)")
    public void operationLogPointcut() {
    }

    @Around("operationLogPointcut() && @annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        Instant start = Instant.now();
        OperationLogEntry entry = buildEntry(joinPoint, operationLog);
        try {
            Object result = joinPoint.proceed();
            entry.setResult(OperationResult.SUCCESS);
            entry.setResponse(serialize(result));
            return result;
        } catch (Throwable ex) {
            entry.setResult(OperationResult.FAILURE);
            entry.setErrorMessage(ex.getMessage());
            throw ex;
        } finally {
            long costMillis = Duration.between(start, Instant.now()).toMillis();
            String baseAction = StringUtils.hasText(entry.getAction()) ? entry.getAction() : joinPoint.getSignature().getName();
            entry.setAction(baseAction + "(" + costMillis + "ms)");
            logRepository.save(entry);
        }
    }

    private OperationLogEntry buildEntry(JoinPoint joinPoint, OperationLog operationLog) {
        OperationLogEntry entry = new OperationLogEntry();
        entry.setOperator(SecurityUtils.getCurrentUsername());
        entry.setTitle(operationLog.title());
        entry.setAction(resolveAction(operationLog, joinPoint));
        entry.setType(operationLog.type());
        entry.setResourceId(resolveResourceId(operationLog.resourceId(), joinPoint));
        HttpServletRequest request = currentRequest().orElse(null);
        if (request != null) {
            entry.setIp(request.getRemoteAddr());
            entry.setEndpoint(request.getRequestURI());
            entry.setHttpMethod(request.getMethod());
            entry.setRequestParams(truncate(maskQuery(request.getQueryString())));
            if (!"GET".equalsIgnoreCase(request.getMethod())) {
                entry.setRequestBody(extractRequestBody(joinPoint));
            }
        }
        return entry;
    }

    private Optional<HttpServletRequest> currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return Optional.ofNullable(servletAttributes.getRequest());
        }
        return Optional.empty();
    }

    private String extractRequestBody(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> filtered = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
                continue;
            }
            if (arg instanceof MultipartFile file) {
                filtered.add(Map.of(
                        "file", file.getOriginalFilename(),
                        "size", file.getSize()
                ));
                continue;
            }
            filtered.add(arg);
        }
        return filtered.isEmpty() ? null : serialize(filtered);
    }

    private String serialize(Object target) {
        if (target == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(target);
            return truncate(sanitize(json));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object for operation log", e);
            return truncate(String.valueOf(target));
        }
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return SENSITIVE_PATTERN.matcher(value).replaceAll("\"$1\":\"***\"");
    }

    private String maskQuery(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return SENSITIVE_QUERY_PATTERN.matcher(value).replaceAll("$1=***");
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_FIELD_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_FIELD_LENGTH) + "...";
    }

    private String resolveAction(OperationLog annotation, JoinPoint joinPoint) {
        if (StringUtils.hasText(annotation.action())) {
            return annotation.action();
        }
        return joinPoint.getSignature().getName();
    }

    private String resolveResourceId(String expression, JoinPoint joinPoint) {
        if (!StringUtils.hasText(expression) || !expression.contains("#")) {
            return expression;
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
        if (paramNames == null) {
            return expression;
        }
        StandardEvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        try {
            Object value = parser.parseExpression(expression).getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ex) {
            log.debug("Failed to evaluate resourceId expression: {}", expression, ex);
            return expression;
        }
    }
}
