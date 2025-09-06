package com.pagerealm.shoppingcart.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartSummaryResponse {
    private Integer cartTotalAmount;
    private Integer expectedPointsReward;
}

