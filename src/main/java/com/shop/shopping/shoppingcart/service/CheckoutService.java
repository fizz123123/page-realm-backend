package com.shop.shopping.shoppingcart.service;

import com.shop.shopping.shoppingcart.dto.request.ApplyCouponRequest;
import com.shop.shopping.shoppingcart.dto.request.ApplyPointsRequest;
import com.shop.shopping.shoppingcart.dto.response.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

public interface CheckoutService {
    /**
     *
     * @param userId
     * @return
     */
    CartSummaryResponse getCartSummary(Long userId);


    /**
     * 顯示會員等級資訊
     * @param userId
     * @return
     */
    MembershipTierResponse getMembershipTierInfo(Long userId);

    /**
     *
     * @param userId
     * @return
     */
    PointsInfoResponse getAvailablePoints(Long userId);

    /**
     *
     * @param userId
     * @param request
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    PointsDeductionResponse applyPoints(Long userId, ApplyPointsRequest request);


    /**
     *
     * @param userId
     * @param request
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    CouponDeductionResponse applyCoupon(Long userId, ApplyCouponRequest request);


    /**
     *
     * @param userId
     */
    @Transactional(rollbackFor = Exception.class)
    Map<String, String> createOrder(Long userId);


    @Transactional(rollbackFor = Exception.class)
    void notify(String tradeNo, String paymentType, LocalDateTime paymentDate);



}
