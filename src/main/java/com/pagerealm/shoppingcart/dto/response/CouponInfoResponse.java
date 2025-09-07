package com.pagerealm.shoppingcart.dto.response;

import com.pagerealm.coupons_points.entity.Coupon;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CouponInfoResponse {
    private String name;
    private String discountType;
    private Integer discountValue;
    private String code;

    public CouponInfoResponse(String name,
                              Coupon.DiscountType discountType,
                              Integer discountValue,
                              String code
    ) {

        this.name = name;
        this.discountType = discountType.toString();
        this.discountValue = discountValue;
        this .code = code;
    }
}

