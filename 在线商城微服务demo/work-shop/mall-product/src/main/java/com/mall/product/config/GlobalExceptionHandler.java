package com.mall.product.config;

import com.mall.common.response.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Pattern CODE_PATTERN = Pattern.compile("code\\s*=\\s*(\\d{3,5})", Pattern.CASE_INSENSITIVE);

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        String message = e.getMessage() == null ? "运行时异常" : e.getMessage();
        Matcher matcher = CODE_PATTERN.matcher(message);
        if (matcher.find()) {
            int code = Integer.parseInt(matcher.group(1));
            return Result.error(code, message);
        }
        return Result.error(message);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return Result.error("服务器内部错误: " + e.getMessage());
    }
}
