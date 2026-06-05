package com.campus.trade.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SearchKeywordParserTest {

    @Test
    void parseShouldSplitAndNormalizeTokens() {
        List<String> tokens = SearchKeywordParser.parse("iPad 2021, 128G  九成新");
        assertThat(tokens).containsExactly("ipad", "2021", "128g", "九成新");
    }

    @Test
    void parseShouldLimitMaximumTokensAndLength() {
        String keyword = "aLongKeywordValueThatShouldBeTrimmed because there are many words split here";
        List<String> tokens = SearchKeywordParser.parse(keyword);
        assertThat(tokens).hasSizeLessThanOrEqualTo(5);
        assertThat(tokens.get(0).length()).isLessThanOrEqualTo(32);
    }
}
