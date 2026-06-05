package com.campus.trade.repository;

import com.campus.trade.model.entity.SearchHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    Optional<SearchHistory> findByUserIdAndNormalizedKeyword(Long userId, String normalizedKeyword);

    List<SearchHistory> findTop20ByUserIdOrderByLastSearchedAtDesc(Long userId);

    List<SearchHistory> findByUserIdOrderByLastSearchedAtDesc(Long userId);
}
