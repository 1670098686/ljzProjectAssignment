package com.campus.trade.repository;

import com.campus.trade.model.entity.Favorite;
import com.campus.trade.repository.projection.CategoryCountView;
import com.campus.trade.repository.projection.FavoriteCountView;
import com.campus.trade.repository.projection.FavoriteStatusCountView;
import com.campus.trade.repository.projection.ProductHeatView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long>, JpaSpecificationExecutor<Favorite> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

        @EntityGraph(attributePaths = {"product", "product.seller"})
        Page<Favorite> findByUserId(Long userId, Pageable pageable);

        @Override
        @EntityGraph(attributePaths = {"product", "product.seller"})
        Page<Favorite> findAll(Specification<Favorite> spec, Pageable pageable);

    long countByCreateTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByProductId(Long productId);

    long countByUserId(Long userId);

    @Query("select f.product.id from Favorite f where f.user.id = :userId and f.product.id in :productIds")
    List<Long> findProductIdsByUserIdAndProductIdIn(@Param("userId") Long userId,
                                                    @Param("productIds") Collection<Long> productIds);

    @Query("select f.product.category as category, count(f) as total " +
            "from Favorite f where f.user.id = :userId group by f.product.category order by total desc")
    List<CategoryCountView> countFavoriteCategories(@Param("userId") Long userId);

    @Query("select f.product.id from Favorite f where f.user.id = :userId")
    List<Long> findProductIdsByUserId(@Param("userId") Long userId);

    @Query("select f.product.id as productId, f.product.title as productTitle, count(f) as total " +
            "from Favorite f where f.product.status = com.campus.trade.model.enums.ProductStatus.ON_SALE " +
            "and f.product.auditStatus = com.campus.trade.model.enums.AuditStatus.APPROVED " +
            "group by f.product.id, f.product.title order by total desc")
    List<ProductHeatView> topFavoritedProducts(Pageable pageable);

    @Query("select f.product.id as productId, count(f) as total from Favorite f where f.product.id in :productIds group by f.product.id")
    List<FavoriteCountView> countByProductIds(@Param("productIds") Collection<Long> productIds);

    @Query("select f.product.status as status, count(f) as total from Favorite f where f.user.id = :userId group by f.product.status")
    List<FavoriteStatusCountView> countFavoriteStatus(@Param("userId") Long userId);

    Favorite findTopByUserIdOrderByCreateTimeDesc(Long userId);
}
