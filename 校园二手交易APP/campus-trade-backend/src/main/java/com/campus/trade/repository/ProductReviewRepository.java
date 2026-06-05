package com.campus.trade.repository;

import com.campus.trade.model.entity.ProductReview;
import com.campus.trade.model.enums.ReviewStatus;
import com.campus.trade.repository.projection.ProductRatingAggregate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    boolean existsByOrderId(Long orderId);

    Page<ProductReview> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    long countByProductIdAndStatus(Long productId, ReviewStatus status);

    @Query("select r.product.id as productId, avg(r.rating) as averageRating, count(r) as ratingCount " +
            "from ProductReview r where r.product.id in :productIds and r.status = :status group by r.product.id")
    List<ProductRatingAggregate> aggregateRatings(@Param("productIds") Collection<Long> productIds,
                                                  @Param("status") ReviewStatus status);

    @Query("select coalesce(avg(r.rating),0) from ProductReview r where r.product.id = :productId and r.status = :status")
    double findAverageRating(@Param("productId") Long productId, @Param("status") ReviewStatus status);

    List<ProductReview> findTop3ByProductIdAndStatusOrderByCreateTimeDesc(Long productId, ReviewStatus status);

    long countByStatusAndCreateTimeBetween(ReviewStatus status, LocalDateTime start, LocalDateTime end);
}
