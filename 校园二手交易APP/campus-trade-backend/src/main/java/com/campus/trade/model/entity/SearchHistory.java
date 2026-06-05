package com.campus.trade.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_history",
    uniqueConstraints = @UniqueConstraint(name = "uk_search_history_user_keyword", columnNames = {"user_id", "normalized_keyword"}))
public class SearchHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
