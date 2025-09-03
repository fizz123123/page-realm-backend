package com.shop.shopping.shoppingcart.repository;

import com.shop.shopping.shoppingcart.entity.WishlistItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishlistItemRepository extends JpaRepository<WishlistItems,Long> {
    List<WishlistItems> findByWishlist_Id(Long wishlistId);
}
