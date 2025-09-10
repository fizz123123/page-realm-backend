package com.shop.shopping.shoppingcart.repository;

import com.shop.shopping.shoppingcart.entity.Cart;
import com.shop.shopping.shoppingcart.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndStatus(Long userId, CartStatus status);
}
