package com.pagerealm.coupons_points.dto.coupon;

import com.pagerealm.coupons_points.entity.Coupon;
import com.pagerealm.coupons_points.entity.CouponRedemption;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class UserCouponResponse {
    private LocalDate startsAt;
    private LocalDate endsAt;
    private String name;
    private String discountType;
    private Integer discountValue;
    private String status;

    public UserCouponResponse(LocalDateTime startsAt,
                              LocalDateTime endsAt,
                              String name,
                              Coupon.DiscountType discountType,
                              Integer discountValue,
                              CouponRedemption.RedemptionStatus status) {
        this.startsAt = startsAt.toLocalDate();
        this.endsAt = endsAt.toLocalDate();
        this.name = name;
        this.discountType = discountType.toString();
        this.discountValue = discountValue;
        this.status = status.toString();
    }

}
