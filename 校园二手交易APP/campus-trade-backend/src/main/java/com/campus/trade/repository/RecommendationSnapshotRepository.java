package com.campus.trade.repository;

import com.campus.trade.model.entity.RecommendationSnapshot;
import com.campus.trade.model.enums.RecommendationScene;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RecommendationSnapshotRepository extends JpaRepository<RecommendationSnapshot, Long> {

    List<RecommendationSnapshot> findByUserIdAndSceneAndExpireTimeAfterOrderByScoreDesc(Long userId,
                                                                                        RecommendationScene scene,
                                                                                        LocalDateTime now);

    List<RecommendationSnapshot> findByUserIsNullAndSceneAndExpireTimeAfterOrderByScoreDesc(RecommendationScene scene,
                                                                                            LocalDateTime now);

    void deleteByExpireTimeBefore(LocalDateTime time);
}
