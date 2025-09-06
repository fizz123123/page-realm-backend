package com.pagerealm.shoppingcart.service;

import com.pagerealm.shoppingcart.dto.request.AddItemToCartfromWishRequest;
import com.pagerealm.shoppingcart.dto.request.AddItemToWishRequest;
import com.pagerealm.shoppingcart.dto.request.RemoveItemFromWishRequest;
import com.pagerealm.shoppingcart.dto.response.WishResponse;
import org.springframework.transaction.annotation.Transactional;

public interface WishlistService {

    /**
     * 合併匿名願望清單到用戶願望清單
     * @param anonWishlistId
     * @param userId
     */
    @Transactional(rollbackFor = Exception.class)
    void mergeAnonWishToUserWish(String anonWishlistId, Long userId);

    /**
     * 登入會員將商品加入願望清單
     * @param userId
     * @param request
     */
    @Transactional(rollbackFor = Exception.class)
    void addItemToWish(Long userId, AddItemToWishRequest request);

    /**
     * 根據用戶 ID 獲取願望清單
     * @param userId
     * @return WishResponse
     */
    WishResponse getWish(Long userId);

    /**
     * 根據用戶 ID 的請求刪除願望清單中的商品
     * @param userId
     * @param request
     */
    @Transactional(rollbackFor = Exception.class)
    void removeItemFromWish(Long userId, RemoveItemFromWishRequest request);

    /**
     * 根據用戶 ID 的請求轉移商品到購物車並刪除願望清單中的商品
     * @param userId
     */
    @Transactional(rollbackFor = Exception.class)
    void addItemToCartfromWish(Long userId, AddItemToCartfromWishRequest request);

}
