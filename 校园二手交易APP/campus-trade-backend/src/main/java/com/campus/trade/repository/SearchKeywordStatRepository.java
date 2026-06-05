package com.campus.trade.repository;

import com.campus.trade.model.entity.SearchKeywordStat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchKeywordStatRepository extends JpaRepository<SearchKeywordStat, Long> {

    Optional<SearchKeywordStat> findByNormalizedKeyword(String normalizedKeyword);

    List<SearchKeywordStat> findAllByOrderBySearchCountDescLastSearchedAtDesc(Pageable pageable);

    List<SearchKeywordStat> findAllByNormalizedKeywordStartingWithOrderBySearchCountDescLastSearchedAtDesc(String normalizedKeyword,
                                                                                                           Pageable pageable);
}
