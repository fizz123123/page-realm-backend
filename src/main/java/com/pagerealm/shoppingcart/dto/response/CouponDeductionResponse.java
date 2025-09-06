package com.pagerealm.shoppingcart.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CouponDeductionResponse {

    private String couponCode;
    private Integer deductionAmount;
    private  Boolean result;
    private  String message;
}
