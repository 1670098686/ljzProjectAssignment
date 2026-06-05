package com.campus.trade.repository;

import com.campus.trade.model.entity.RecommendationEvent;
import com.campus.trade.model.enums.RecommendationScene;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationEventRepository extends JpaRepository<RecommendationEvent, Long> {

    @Query("select distinct e.product.id from RecommendationEvent e " +
	    "where e.user is not null and e.user.id = :userId " +
	    "and (:scene is null or e.scene = :scene) and e.createTime >= :since")
    List<Long> findRecentProductIds(@Param("userId") Long userId,
				    @Param("scene") RecommendationScene scene,
				    @Param("since") LocalDateTime since);
}
