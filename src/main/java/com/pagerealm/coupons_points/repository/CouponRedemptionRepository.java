package com.pagerealm.coupons_points.repository;

import com.pagerealm.coupons_points.dto.coupon.UserCouponResponse;
import com.pagerealm.coupons_points.entity.Coupon;
import com.pagerealm.coupons_points.entity.CouponRedemption;
import com.pagerealm.coupons_points.entity.CouponRedemption.RedemptionStatus;
import com.pagerealm.shoppingcart.dto.response.CouponInfoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    long countByCouponIdAndStatus(Long couponId, RedemptionStatus status);
    long countByCouponIdAndUserIdAndStatus(Long couponId, Long userId, RedemptionStatus status);

    Optional<CouponRedemption> findByUserIdAndStatus(Long userId, RedemptionStatus status);

    boolean existsByOrderIdAndCouponId(Long orderId, Long couponId);
    Page<CouponRedemption> findAllByUserIdOrderByRedeemedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT new com.pagerealm.shoppingcart.dto.response.CouponInfoResponse( c.name, c.discountType, c.discountValue)"+
            "FROM CouponRedemption cr " +
            "JOIN cr.coupon c " +
            "WHERE cr.userId = :userId AND cr.status = 'HOLD' ")
    List<CouponInfoResponse> findValidCouponByUserId(@Param("userId") Long userId);

    @Query("SELECT new com.pagerealm.coupons_points.dto.coupon.UserCouponResponse( c.startsAt, c.endsAt, c.name, c.discountType,c.discountValue, cr.status) " +
            "FROM CouponRedemption cr " +
            "JOIN cr.coupon c " +
            "WHERE cr.userId = :userId ORDER BY cr.redeemedAt DESC")
    Page<UserCouponResponse> findAllDTOByUserId(@Param("userId") Long userId, Pageable pageable);

    boolean existsByuserIdAndCoupon(Long userId, Coupon coupon);
}

