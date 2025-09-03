package com.shop.shopping.shoppingcart.repository;

import com.shop.shopping.shoppingcart.entity.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {

}
