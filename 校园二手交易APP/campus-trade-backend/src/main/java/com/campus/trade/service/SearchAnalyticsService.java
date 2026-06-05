package com.campus.trade.service;

import com.campus.trade.dto.product.HotKeywordResponse;
import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.dto.product.SearchHistoryResponse;
import com.campus.trade.model.entity.SearchHistory;
import com.campus.trade.model.entity.SearchKeywordStat;
import com.campus.trade.model.entity.User;
import com.campus.trade.repository.SearchHistoryRepository;
import com.campus.trade.repository.SearchKeywordStatRepository;
import com.campus.trade.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class SearchAnalyticsService {

    private static final int HISTORY_LIMIT = 20;
    private static final int MAX_HOT_LIMIT = 20;

    private final SearchHistoryRepository historyRepository;
    private final SearchKeywordStatRepository keywordStatRepository;
    private final UserRepository userRepository;

    public SearchAnalyticsService(SearchHistoryRepository historyRepository,
                                  SearchKeywordStatRepository keywordStatRepository,
                                  UserRepository userRepository) {
        this.historyRepository = historyRepository;
        this.keywordStatRepository = keywordStatRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordSearch(String username, String keyword, List<ProductResponse> results) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        String normalized = normalize(keyword);
        LocalDateTime now = LocalDateTime.now();
        resolveUser(username).ifPresent(user -> {
            SearchHistory history = historyRepository.findByUserIdAndNormalizedKeyword(user.getId(), normalized)
                    .orElseGet(() -> {
                        SearchHistory entity = new SearchHistory();
                        entity.setUser(user);
                        entity.setNormalizedKeyword(normalized);
                        entity.setSearchCount(0);
                        return entity;
                    });
            history.setKeyword(keyword.trim());
            history.setNormalizedKeyword(normalized);
            history.setSearchCount(history.getSearchCount() + 1);
            history.setLastSearchedAt(now);
            historyRepository.save(history);
            trimHistory(user.getId());
        });
        SearchKeywordStat stat = keywordStatRepository.findByNormalizedKeyword(normalized)
                .orElseGet(() -> {
                    SearchKeywordStat entity = new SearchKeywordStat();
                    entity.setNormalizedKeyword(normalized);
                    entity.setSearchCount(0);
                    return entity;
                });
        stat.setKeyword(keyword.trim());
        stat.setNormalizedKeyword(normalized);
        stat.setSearchCount(stat.getSearchCount() + 1);
        stat.setLastSearchedAt(now);
        keywordStatRepository.save(stat);
    }

    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> getHistory(String username, int limit) {
        Optional<User> user = resolveUser(username);
        if (user.isEmpty()) {
            return List.of();
        }
        List<SearchHistory> entries = historyRepository.findTop20ByUserIdOrderByLastSearchedAtDesc(user.get().getId());
        return entries.stream()
                .limit(safeLimit(limit, HISTORY_LIMIT))
                .map(entry -> new SearchHistoryResponse(entry.getKeyword(), entry.getLastSearchedAt(), entry.getSearchCount()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getRecentKeywords(String username, int limit) {
        Optional<User> user = resolveUser(username);
        if (user.isEmpty()) {
            return List.of();
        }
        List<SearchHistory> entries = historyRepository.findTop20ByUserIdOrderByLastSearchedAtDesc(user.get().getId());
        return entries.stream()
                .limit(safeLimit(limit, HISTORY_LIMIT))
                .map(SearchHistory::getKeyword)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HotKeywordResponse> getHotKeywordsDetail(int limit) {
        List<SearchKeywordStat> stats = keywordStatRepository.findAllByOrderBySearchCountDescLastSearchedAtDesc(
                PageRequest.of(0, safeLimit(limit, MAX_HOT_LIMIT)));
        if (CollectionUtils.isEmpty(stats)) {
            return List.of();
        }
        return stats.stream()
                .map(stat -> new HotKeywordResponse(stat.getKeyword(), stat.getSearchCount(), stat.getLastSearchedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getHotKeywords(int limit) {
        return getHotKeywordsDetail(limit).stream()
                .map(HotKeywordResponse::getKeyword)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getRelatedKeywords(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return getHotKeywords(limit);
        }
        String normalized = normalize(keyword);
        List<SearchKeywordStat> stats = keywordStatRepository
                .findAllByNormalizedKeywordStartingWithOrderBySearchCountDescLastSearchedAtDesc(
                        normalized, PageRequest.of(0, safeLimit(limit, MAX_HOT_LIMIT)));
        if (CollectionUtils.isEmpty(stats)) {
            return getHotKeywords(limit);
        }
        return stats.stream()
                .map(SearchKeywordStat::getKeyword)
                .distinct()
                .limit(safeLimit(limit, MAX_HOT_LIMIT))
                .collect(Collectors.toList());
    }

    private Optional<User> resolveUser(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }

    private void trimHistory(Long userId) {
        List<SearchHistory> histories = historyRepository.findByUserIdOrderByLastSearchedAtDesc(userId);
        if (histories.size() <= HISTORY_LIMIT) {
            return;
        }
        List<SearchHistory> obsolete = histories.subList(HISTORY_LIMIT, histories.size());
        historyRepository.deleteAll(obsolete);
    }

    private int safeLimit(int limit, int max) {
        int safe = limit <= 0 ? max : limit;
        return Math.min(safe, max);
    }

    private String normalize(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    }
}
