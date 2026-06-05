package com.campus.trade.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_keyword_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_search_keyword_stats_normalized", columnNames = "normalized_keyword"))
public class SearchKeywordStat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword", nullable = false, length = 120)
    private String keyword;

    @Column(name = "normalized_keyword", nullable = false, length = 120)
    private String normalizedKeyword;

    @Column(name = "search_count", nullable = false)
    private long searchCount;

    @Column(name = "last_searched_at", nullable = false)
    private LocalDateTime lastSearchedAt;

    public Long getId() {
        return id;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getNormalizedKeyword() {
        return normalizedKeyword;
    }

    public void setNormalizedKeyword(String normalizedKeyword) {
        this.normalizedKeyword = normalizedKeyword;
    }

    public long getSearchCount() {
        return searchCount;
    }

    public void setSearchCount(long searchCount) {
        this.searchCount = searchCount;
    }

    public LocalDateTime getLastSearchedAt() {
        return lastSearchedAt;
    }

    public void setLastSearchedAt(LocalDateTime lastSearchedAt) {
        this.lastSearchedAt = lastSearchedAt;
    }
}
