package com.pagerealm.books.controller;

import com.pagerealm.books.dto.UploadResponse;
import com.pagerealm.books.entity.Book;
import com.pagerealm.books.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // 僅上傳並回傳 URL
    @PostMapping("/cover")
    public ResponseEntity<UploadResponse> uploadCover(@RequestParam("file") MultipartFile file) {
        String url = bookService.uploadCover(file);
        return ResponseEntity.ok(new UploadResponse(url));
    }

    // 上傳並寫回指定書籍的封面 URL
    @PostMapping("/{id}/cover")
    public ResponseEntity<Book> uploadAndAttachCover(@PathVariable Long id,
                                                     @RequestParam("file") MultipartFile file) {
        String url = bookService.uploadCover(file);
        Book updated = bookService.updateCover(id, url);
        return ResponseEntity.ok(updated);
    }
}
