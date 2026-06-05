package com.campus.trade.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public final class SearchKeywordParser {

    private static final Pattern DELIMITER = Pattern.compile("[\\s,，;；]+");
    private static final int MAX_KEYWORDS = 5;
    private static final int MAX_KEYWORD_LENGTH = 32;

    private SearchKeywordParser() {
    }

    public static List<String> parse(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        return Arrays.stream(DELIMITER.split(keyword.trim()))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(token -> token.substring(0, Math.min(token.length(), MAX_KEYWORD_LENGTH)))
                .limit(MAX_KEYWORDS)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }
}
