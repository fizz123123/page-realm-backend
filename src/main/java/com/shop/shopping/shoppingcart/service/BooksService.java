package com.shop.shopping.shoppingcart.service;

import com.shop.shopping.shoppingcart.dto.BooksIndexDTO;
import com.shop.shopping.shoppingcart.repository.BooksRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BooksService {
    private BooksRepository booksRepository;

    public BooksService(BooksRepository bookRepository) {
        this.booksRepository = bookRepository;
    }

    public List<BooksIndexDTO> getAllBooks() {
        return booksRepository.findAll().stream()
                .map(book -> new BooksIndexDTO(
                        book.getId(),
                        book.getTitle(),
                        book.getPublisherName(),
                        book.getPrice(),
                        book.getFormat(),
                        book.getCoverImageUrl()
                ))
                .collect(Collectors.toList());
    }
}