package com.pagerealm.shoppingcart.controller;

import com.pagerealm.shoppingcart.dto.BooksIndexDTO;
import com.pagerealm.shoppingcart.service.BooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BooksController {
    @Autowired
    private final BooksService booksService;

    public BooksController(BooksService booksService) {
        this.booksService = booksService;
    }

    @GetMapping("/books")
    public List<BooksIndexDTO> getBooks() {
        return booksService.getAllBooks();
    }
}