package com.campus.trade.repository;

import com.campus.trade.model.entity.Order;
import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.model.enums.RefundStatus;
import com.campus.trade.repository.projection.CategoryCountView;
import com.campus.trade.repository.projection.OrderStatusCountView;
import com.campus.trade.repository.projection.ProductHeatView;
import com.campus.trade.repository.projection.SchoolCountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

        @EntityGraph(attributePaths = {"product", "product.categoryEntity", "buyer", "seller"})
        Optional<Order> findById(Long id);
        
        @EntityGraph(attributePaths = {"product", "product.categoryEntity", "buyer", "seller"})
        Page<Order> findAll(Pageable pageable);

        @EntityGraph(attributePaths = {"product", "product.categoryEntity", "buyer", "seller"})
        Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "product.categoryEntity", "buyer", "seller"})
    Page<Order> findByBuyerId(Long buyerId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "product.categoryEntity", "buyer", "seller"})
    Page<Order> findBySellerId(Long sellerId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "product.categoryEntity", "buyer", "seller"})
    Page<Order> findByBuyerIdAndStatus(Long buyerId, OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "product.categoryEntity", "buyer", "seller"})
    Page<Order> findBySellerIdAndStatus(Long sellerId, OrderStatus status, Pageable pageable);

    List<Order> findAllByStatusAndCreateTimeBefore(OrderStatus status, LocalDateTime createTime);

    long countByCreateTimeAfter(LocalDateTime time);

    long countByCreateTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByStatus(OrderStatus status);

    long countByRefundStatus(RefundStatus status);

        long countByStatusAndCreateTimeBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);

    @Query("select coalesce(sum(o.price),0) from Order o where o.status = :status")
    BigDecimal sumTotalPriceByStatus(@Param("status") OrderStatus status);

        @Query("select coalesce(sum(o.price),0) from Order o where o.status = :status and o.createTime between :start and :end")
        BigDecimal sumTotalPriceByStatusAndCreateTimeBetween(@Param("status") OrderStatus status,
                                                                                                                 @Param("start") LocalDateTime start,
                                                                                                                 @Param("end") LocalDateTime end);

    @Query("select o.status as status, count(o) as total from Order o group by o.status")
    List<OrderStatusCountView> aggregateStatusCounts();

    @Query("select o.product.category as category, count(o) as total from Order o " +
            "where o.status = com.campus.trade.model.enums.OrderStatus.COMPLETED " +
            "group by o.product.category order by total desc")
    List<CategoryCountView> topCategoriesByCompletedOrders(Pageable pageable);

    @Query("select o.buyer.school as school, count(o) as total from Order o " +
            "where o.status = com.campus.trade.model.enums.OrderStatus.COMPLETED " +
            "and o.buyer.school is not null " +
            "group by o.buyer.school order by total desc")
    List<SchoolCountView> topSchoolsByOrders(Pageable pageable);

    @Query("select o.product.category as category, count(o) as total from Order o " +
            "where o.buyer.id = :buyerId and o.product.category is not null " +
            "group by o.product.category order by total desc")
    List<CategoryCountView> topCategoriesByBuyer(@Param("buyerId") Long buyerId, Pageable pageable);

    @Query("select distinct o.product.id from Order o " +
            "where o.buyer.id = :buyerId " +
            "and o.status = com.campus.trade.model.enums.OrderStatus.COMPLETED")
    List<Long> findCompletedProductIdsByBuyer(@Param("buyerId") Long buyerId);

    @Query("select count(distinct o.buyer.id) from Order o where o.createTime between :start and :end")
    long countDistinctBuyersBetween(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    @Query("select count(distinct o.product.seller.id) from Order o where o.createTime between :start and :end")
    long countDistinctSellersBetween(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    @Query("select o.product.id as productId, o.product.title as productTitle, count(o) as total from Order o " +
            "where o.status = com.campus.trade.model.enums.OrderStatus.COMPLETED " +
            "group by o.product.id, o.product.title order by total desc")
    List<ProductHeatView> topProductsByCompletedOrders(Pageable pageable);
}
