package com.pagerealm.shoppingcart.service.impl;

import com.pagerealm.books.entity.Book;
import com.pagerealm.books.repository.BookRepository;
import com.pagerealm.shoppingcart.dto.request.AddItemToCartfromWishRequest;
import com.pagerealm.shoppingcart.dto.request.AddItemToWishRequest;
import com.pagerealm.shoppingcart.dto.request.RemoveItemFromWishRequest;
import com.pagerealm.shoppingcart.dto.response.WishItemResponse;
import com.pagerealm.shoppingcart.dto.response.WishResponse;
import com.pagerealm.shoppingcart.entity.*;
import com.pagerealm.shoppingcart.repository.AnonWishlistRedisRepository;
import com.pagerealm.shoppingcart.repository.CartRepository;
import com.pagerealm.shoppingcart.repository.WishlistItemRepository;
import com.pagerealm.shoppingcart.repository.WishlistRepository;
import com.pagerealm.shoppingcart.service.WishlistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class WishlistServiceImpl implements WishlistService {
    private final WishlistRepository wishlistRepository;
    private final CartRepository cartRepository;
    private final AnonWishlistRedisRepository anonWishlistRedisRepository;
    private final BookRepository booksRepository;
    private final WishlistItemRepository wishlistItemRepository;

    public WishlistServiceImpl(WishlistRepository wishlistRepository,
                               CartRepository cartRepository,
                               AnonWishlistRedisRepository anonWishlistRedisRepository,
                               BookRepository booksRepository, WishlistItemRepository wishlistItemRepository) {
        this.wishlistRepository = wishlistRepository;
        this.cartRepository = cartRepository;
        this.anonWishlistRedisRepository = anonWishlistRedisRepository;
        this.booksRepository = booksRepository;
        this.wishlistItemRepository = wishlistItemRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeAnonWishToUserWish(String anonWishlistId, Long userId) {
        Set<String> anonBookIds = anonWishlistRedisRepository.getAll(anonWishlistId, "wishlist");
        if (anonBookIds == null || anonBookIds.isEmpty()) return;
        Wishlist userWish = wishlistRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseGet(() -> {
                    Wishlist w = new Wishlist();
                    w.setUserId(userId);
                    w.setStatus("ACTIVE");
                    w.setItems(new ArrayList<>());
                    return wishlistRepository.save(w);
                });
        Set<Long> userBookIds = new HashSet<>();
        for (WishlistItems item : userWish.getItems()) {
            userBookIds.add(item.getBook().getId());
        }

        for (String bookIdStr : anonBookIds) {
            Long bookId = Long.valueOf(bookIdStr);
            if (!userBookIds.contains(bookId)) {
                Book book = booksRepository.findById(bookId).orElse(null);
                if (book != null) {
                    WishlistItems newItem = new WishlistItems();
                    newItem.setBook(book);
                    newItem.setWishlist(userWish);
                    userWish.getItems().add(newItem);
                }
            }
        }
        wishlistRepository.save(userWish);
        anonWishlistRedisRepository.clear(anonWishlistId, "wishlist");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItemToWish(Long userId, AddItemToWishRequest request) {
        if (request == null || request.getBookId() == null) {
            throw new IllegalArgumentException("商品資訊不正確");
        }
        Wishlist wishlist = wishlistRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseGet(() -> {
                    Wishlist w = new Wishlist();
                    w.setUserId(userId);
                    w.setStatus("ACTIVE");
                    w.setItems(new ArrayList<>());
                    return wishlistRepository.save(w);
                });
        boolean exists = wishlist.getItems().stream()
                .anyMatch(i -> i.getBook().getId().equals(request.getBookId()));
        if (!exists) {
            Book book = booksRepository.findById(request.getBookId()).orElseThrow();
            WishlistItems newItem = new WishlistItems();
            newItem.setBook(book);
            newItem.setWishlist(wishlist);
            wishlist.getItems().add(newItem);
            wishlistRepository.save( wishlist);
        }
    }

    @Override
    public WishResponse getWish(Long userId) {
        Wishlist wishlist = wishlistRepository.findByUserIdAndStatus(userId, "ACTIVE").orElse(null);
        WishResponse response = new WishResponse();
        if (wishlist == null) {
            response.setWishId(null);
            response.setUserId(userId);
            response.setItems(Collections.emptyList());
            return response;
        }
        response.setWishId(wishlist.getId());
        response.setUserId(wishlist.getUserId());
        List<WishItemResponse> itemResponses = new ArrayList<>();
        for (int i = 0; i < wishlist.getItems().size(); i++) {
            WishlistItems item = wishlist.getItems().get(i);
            Book book = item.getBook();
            WishItemResponse itemResp = new  WishItemResponse();
            itemResp.setId(item.getId());
            itemResp.setTitle(book.getTitle());
            itemResp.setAuthor(book.getAuthor());
            itemResp.setPrice(book.getListPrice());
            itemResp.setFormat(book.getFormat());
            itemResp.setCoverImageUrl(book.getCoverImageUrl());
            itemResponses.add(itemResp);
        }
        response.setItems(itemResponses);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItemFromWish(Long userId, RemoveItemFromWishRequest request) {
        if (userId == null || request == null || request.getIds() == null|| request.getIds().isEmpty()) {
            throw new IllegalArgumentException("userId 或 wishItemIds 不可為空");
        }
        Wishlist wishlist = wishlistRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElse(null);
        if (wishlist == null) {
            throw new IllegalArgumentException("找不到該用戶的願望清單");
        }
        List<Long> wishItemIds = request.getIds();
        List<WishlistItems> items = wishlist.getItems();
        Iterator<WishlistItems> iterator = items.iterator();
        boolean removed = false;
        while (iterator.hasNext()) {
            WishlistItems item = iterator.next();
            if (item.getId() != null && wishItemIds.contains(item.getId())) {
                iterator.remove();
                removed = true;
            }
        }
        if (!removed) {
            throw new IllegalArgumentException("願望清單中未找到指定商品");
        }
        wishlistRepository.save(wishlist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItemToCartfromWish(Long userId, AddItemToCartfromWishRequest request) {
        if (userId == null || request == null || request.getIds() == null|| request.getIds().isEmpty()) {
            throw new IllegalArgumentException("userId 或 wishItemIds 不可為空");
        }
        Wishlist wishlist = wishlistRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElse(null);
        if (wishlist == null) {
            throw new IllegalArgumentException("找不到該用戶的願望清單");
        }
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setUserId(userId);
                    c.setStatus(CartStatus.ACTIVE);
                    c.setItems(new ArrayList<>());
                    return cartRepository.save(c);
                });

        List<Long> wishItemIds = request.getIds();
        List<WishlistItems> items = wishlist.getItems();
        Iterator<WishlistItems> iterator = items.iterator();
        boolean removed = false;
        while (iterator.hasNext()) {
            WishlistItems item = iterator.next();
            if (item.getId() != null && wishItemIds.contains(item.getId())) {

                Book book = booksRepository.findById(item.getBook().getId()).orElseThrow();
                boolean alreadyInCart = cart.getItems().stream()
                        .anyMatch(ci -> ci.getBook().getId().equals(book.getId()));
                if (alreadyInCart) {
                    iterator.remove();
                    removed = true;
                    continue;
                }
                iterator.remove();
                CartItem newItem = new CartItem();
                newItem.setBook(book);
                newItem.setCart(cart);
                newItem.setSnapshotPrice(book.getListPrice());
                cart.getItems().add(newItem);
                cartRepository.save(cart);
                removed = true;
            }
        }
        if (!removed) {
            throw new IllegalArgumentException("願望清單中未找到指定商品");
        }
        wishlistRepository.save(wishlist);
    }
}
