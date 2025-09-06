package com.pagerealm.shoppingcart.repository;

import com.pagerealm.shoppingcart.entity.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {

}
