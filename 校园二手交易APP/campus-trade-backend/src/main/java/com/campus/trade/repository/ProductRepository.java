package com.campus.trade.repository;

import com.campus.trade.model.entity.Product;
import com.campus.trade.model.enums.AuditStatus;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.repository.projection.CategoryCountView;
import com.campus.trade.repository.projection.ProductHeatView;
import com.campus.trade.repository.projection.SchoolCountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @EntityGraph(attributePaths = {"seller", "categoryEntity"})
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"seller", "categoryEntity"})
    Page<Product> findByAuditStatus(AuditStatus auditStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"seller", "categoryEntity"})
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    long countByAuditStatus(AuditStatus auditStatus);

    @Override
    @EntityGraph(attributePaths = {"seller", "categoryEntity"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

        long countByStatus(ProductStatus status);

        @Query("select p.category as category, count(p) as total " +
            "from Product p where p.auditStatus = :auditStatus " +
            "group by p.category order by total desc")
        List<CategoryCountView> countApprovedProductsByCategory(@Param("auditStatus") AuditStatus auditStatus,
                                    Pageable pageable);

            @Query("select p.seller.school as school, count(p) as total " +
                "from Product p where p.auditStatus = :auditStatus and p.seller.school is not null " +
                "group by p.seller.school order by total desc")
            List<SchoolCountView> countApprovedProductsBySchool(@Param("auditStatus") AuditStatus auditStatus,
                                    Pageable pageable);

        List<Product> findTop5ByTitleContainingIgnoreCaseOrderByViewCountDesc(String keyword);

        @EntityGraph(attributePaths = {"seller", "categoryEntity"})
        Page<Product> findByCategoryInAndStatusInAndAuditStatus(Collection<ProductCategory> categories,
                                    Collection<ProductStatus> statuses,
                                    AuditStatus auditStatus,
                                    Pageable pageable);

        @EntityGraph(attributePaths = {"seller", "categoryEntity"})
        Page<Product> findByStatusInAndAuditStatus(Collection<ProductStatus> statuses,
                               AuditStatus auditStatus,
                               Pageable pageable);

        @EntityGraph(attributePaths = {"seller", "categoryEntity"})
        Page<Product> findByStatusAndAuditStatus(ProductStatus status, AuditStatus auditStatus, Pageable pageable);

        long countByCreateTimeBetween(LocalDateTime start, LocalDateTime end);

        @Query("select p.id as productId, p.title as productTitle, coalesce(p.viewCount,0) as total " +
            "from Product p where p.status = com.campus.trade.model.enums.ProductStatus.ON_SALE " +
            "and p.auditStatus = com.campus.trade.model.enums.AuditStatus.APPROVED order by p.viewCount desc")
        List<ProductHeatView> topViewedProducts(Pageable pageable);

        List<Product> findByIdInAndSellerId(Collection<Long> ids, Long sellerId);
}
