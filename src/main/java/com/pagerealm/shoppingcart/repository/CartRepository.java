package com.pagerealm.shoppingcart.repository;

import com.pagerealm.shoppingcart.entity.Cart;
import com.pagerealm.shoppingcart.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndStatus(Long userId, CartStatus status);
}
