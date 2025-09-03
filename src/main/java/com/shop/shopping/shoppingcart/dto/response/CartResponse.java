package com.shop.shopping.shoppingcart.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter @Setter @ToString
public class CartResponse {
    private Long cartId;
    private Long userId;
    private List<CartItemResponse> items;
}