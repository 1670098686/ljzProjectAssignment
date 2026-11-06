package com.mall.review.repository;

import com.mall.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);
    List<Review> findByUserId(Long userId);
    List<Review> findByProductIdAndRating(Long productId, Integer rating);
    boolean existsByUserIdAndOrderIdAndProductId(Long userId, Long orderId, Long productId);
}
