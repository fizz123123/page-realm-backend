package com.shop.shopping.shoppingcart.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MembershipTierResponse {
    private String currentTier;
    private LocalDateTime tierExpireAt;
    private Integer amountToNextTier;
    private String nextTier;
}

