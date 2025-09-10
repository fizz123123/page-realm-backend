package com.shop.shopping.shoppingcart.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class CartItemResponse {
    private Long id;
    private Long bookId;
    private String title;
    private String author;
    private Integer snapshotPrice;
    private String format;
    private String coverImageUrl;
}