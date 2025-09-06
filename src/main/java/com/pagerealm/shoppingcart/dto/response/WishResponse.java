package com.pagerealm.shoppingcart.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter @Setter @ToString
public class WishResponse {
    private Long wishId;
    private Long userId;
    private List<WishItemResponse> items;
}
