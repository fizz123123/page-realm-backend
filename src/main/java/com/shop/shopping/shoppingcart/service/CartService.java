package com.shop.shopping.shoppingcart.service;

import com.shop.shopping.shoppingcart.dto.request.AddItemToCartRequest;
import com.shop.shopping.shoppingcart.dto.request.AddItemToWishfromCartRequest;
import com.shop.shopping.shoppingcart.dto.request.RemoveItemFromCartRequest;
import com.shop.shopping.shoppingcart.dto.response.CartResponse;
import org.springframework.transaction.annotation.Transactional;

public interface CartService {

    /**
     * 合併匿名購物車到用戶購物車
     * @param anonCartId
     * @param userId
     */
    @Transactional(rollbackFor = Exception.class)
    void mergeAnonCartToUserCart(String anonCartId, Long userId);

    /**
     * 登入會員將商品加入購物車
     * @param userId
     * @param request
     */
    @Transactional(rollbackFor = Exception.class)
    void addItemToCart(Long userId, AddItemToCartRequest request);

    /**
     * 根據用戶 ID 獲取購物車
     * @param userId
     * @return CartResponse
     */
    CartResponse getCart(Long userId);

    /**
     * 根據用戶 ID 的請求刪除購物車中的商品
     * @param userId
     * @param request
     */
    @Transactional(rollbackFor = Exception.class)
    void removeItemFromCart(Long userId, RemoveItemFromCartRequest request);

    /**
     * 根據用戶 ID 的請求轉移商品到願望清單並刪除購物車中的商品
     * @param userId
     */
    @Transactional(rollbackFor = Exception.class)
    void addItemToWishfromCart(Long userId, AddItemToWishfromCartRequest request);

}
