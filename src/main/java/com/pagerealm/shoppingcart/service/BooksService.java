package com.pagerealm.shoppingcart.service;

import com.pagerealm.books.repository.BookRepository;
import com.pagerealm.shoppingcart.dto.BooksIndexDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BooksService {
    private BookRepository booksRepository;

    public BooksService(BookRepository booksRepository) {
        this.booksRepository = booksRepository;
    }

    public List<BooksIndexDTO> getAllBooks() {
        return booksRepository.findAll().stream()
                .map(book -> new BooksIndexDTO(
                        book.getId(),
                        book.getTitle(),
                        book.getPublisherName(),
                        book.getListPrice(),
                        book.getFormat(),
                        book.getCoverImageUrl()
                ))
                .collect(Collectors.toList());
    }
}