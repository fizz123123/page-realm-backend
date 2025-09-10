package com.shop.shopping.shoppingcart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "books")
public class Books {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title",length = 500)
    private String title;

    @Column(name = "author")
    private String author;

    @Column(name = "publisher_name")
    private String publisherName;

    @Column(name = "list_price")
    private Integer price;

    @Column(name = "format")
    private String format;

    @Column(name = "status")
    private String status;

    @Column(name = "cover_image_url",length = 500)
    private String coverImageUrl;

    @Column(name = "language")
    private String language;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


}