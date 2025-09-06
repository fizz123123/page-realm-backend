package com.pagerealm.shoppingcart.repository;

import com.pagerealm.shoppingcart.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    Optional<Orders> findByOrderNo(String orderNo);

}
