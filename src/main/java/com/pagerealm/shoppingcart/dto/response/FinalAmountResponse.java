package com.pagerealm.shoppingcart.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinalAmountResponse {
    private Integer cartTotalAmount;
    private Integer pointsDeductionAmount;
    private Integer couponDeductionAmount;
    private Integer finalPayableAmount;
}

