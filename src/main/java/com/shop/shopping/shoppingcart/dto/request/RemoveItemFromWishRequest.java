package com.shop.shopping.shoppingcart.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter @Setter
public class RemoveItemFromWishRequest {
    private List<Long> ids;
}
