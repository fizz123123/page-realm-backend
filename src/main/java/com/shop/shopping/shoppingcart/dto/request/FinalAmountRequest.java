package com.shop.shopping.shoppingcart.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinalAmountRequest {
    private Integer pointsToApply;
    private String couponCode;
}

