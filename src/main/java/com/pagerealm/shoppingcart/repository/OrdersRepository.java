package com.pagerealm.shoppingcart.repository;

import com.pagerealm.order.dto.UserOrderResponse;
import com.pagerealm.shoppingcart.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    Optional<Orders> findByOrderNo(String orderNo);

    @Query("SELECT new com.pagerealm.order.dto.UserOrderResponse(o.orderNo, o.paidAt, o.status, o.payableAmount, o.paymentType) " +
            "FROM Orders o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    Page<UserOrderResponse> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
