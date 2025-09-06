package com.pagerealm.shoppingcart.repository;

import com.pagerealm.shoppingcart.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist,Long> {
    Optional<Wishlist> findByUserIdAndStatus(Long userId, String status);
}
