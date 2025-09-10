package com.shop.shopping.shoppingcart.dto;

import lombok.Getter;

@Getter
public class BooksIndexDTO {
    private Long id;
    private String title;
    private String publisherName;
    private Integer price;
    private String format;
    private String coverImageUrl;

    /**
     * 首頁取得書本資訊
     * @param id
     * @param title
     * @param publisherName
     * @param price
     * @param format
     * @param coverImageUrl
     */
    public BooksIndexDTO(Long id, String title, String publisherName, Integer price, String format, String coverImageUrl) {
        this.id = id;
        this.title = title;
        this.publisherName = publisherName;
        this.price = price;
        this.format = format;
        this.coverImageUrl = coverImageUrl;
    }
}
