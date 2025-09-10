package com.pagerealm.shoppingcart.repository;

import com.pagerealm.order.dto.UserOrderResponse;
import com.pagerealm.shoppingcart.entity.OrderStatus;
import com.pagerealm.shoppingcart.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> , JpaSpecificationExecutor<Orders> {

    Optional<Orders> findByOrderNo(String orderNo);

    @Query("SELECT new com.pagerealm.order.dto.UserOrderResponse(o.orderNo, o.paidAt, o.status, o.payableAmount, o.paymentType) " +
            "FROM Orders o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    Page<UserOrderResponse> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    @EntityGraph(attributePaths = {"items", "items.book"})
    Optional<Orders> findWithItemsById(Long id);

    // 訂單狀態分佈統計（可選日期區間）
    @Query("select o.status as status, count(o) as cnt from Orders o " +
            "where (:from is null or o.createdAt >= :from) and (:to is null or o.createdAt <= :to) " +
            "group by o.status")
    List<StatusCount> countByStatusBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    interface StatusCount {
        OrderStatus getStatus();
        long getCnt();
    }

    // 新增：每日銷售額（以 payableAmount 合計），僅計算已付款/已完成訂單
    @Query("select year(o.createdAt) as y, month(o.createdAt) as m, day(o.createdAt) as d, sum(o.payableAmount) as total " +
            "from Orders o " +
            "where o.status in :statuses and o.createdAt between :from and :to " +
            "group by year(o.createdAt), month(o.createdAt), day(o.createdAt) " +
            "order by year(o.createdAt), month(o.createdAt), day(o.createdAt)")
    List<DailySumRow> sumPayableByDay(
            @Param("statuses") List<OrderStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    interface DailySumRow {
        Integer getY();
        Integer getM();
        Integer getD();
        Long getTotal();
    }
}
