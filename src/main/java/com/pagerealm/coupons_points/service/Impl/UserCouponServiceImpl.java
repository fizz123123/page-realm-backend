package com.pagerealm.coupons_points.service.Impl;

import com.pagerealm.coupons_points.dto.coupon.UserAddCouponRequest;
import com.pagerealm.coupons_points.entity.Coupon;
import com.pagerealm.coupons_points.entity.CouponRedemption;
import com.pagerealm.coupons_points.repository.CouponRedemptionRepository;
import com.pagerealm.coupons_points.repository.CouponRepository;
import com.pagerealm.coupons_points.service.UserCouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserCouponServiceImpl implements UserCouponService {
    private CouponRepository couponRepository;
    private CouponRedemptionRepository couponRedemptionRepository;

    public UserCouponServiceImpl(CouponRepository couponRepository, CouponRedemptionRepository couponRedemptionRepository) {
        this.couponRepository = couponRepository;
        this.couponRedemptionRepository = couponRedemptionRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addCoupon(Long userId, UserAddCouponRequest request) {
        String couponCode = request.getCouponCode();
        Optional<Coupon> couponpt = couponRepository.findByGenericCodeIgnoreCase(couponCode);

        couponpt.ifPresent(coupon -> {
            boolean alreadyRedeemed = couponRedemptionRepository.existsByuserIdAndCoupon(userId, coupon);
            if (alreadyRedeemed) {
                // 可以選擇拋出異常或直接返回
                throw new IllegalArgumentException("使用者已經擁有此優惠券");
            };


            CouponRedemption couponRedemption = new CouponRedemption();
            couponRedemption.setCoupon(coupon);
            couponRedemption.setUserId(userId);
            couponRedemption.setAmountDiscounted(0);
            couponRedemption.setStatus(CouponRedemption.RedemptionStatus.HOLD);
            couponRedemptionRepository.save(couponRedemption);
        });
    }

}
