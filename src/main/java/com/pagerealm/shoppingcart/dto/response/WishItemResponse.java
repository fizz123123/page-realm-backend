package com.pagerealm.shoppingcart.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class WishItemResponse {
    private Long id;
    private Long bookId;
    private String title;
    private String author;
    private Integer price;
    private String format;
    private String coverImageUrl;
}
