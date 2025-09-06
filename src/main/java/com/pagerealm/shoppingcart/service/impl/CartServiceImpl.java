package com.pagerealm.shoppingcart.service.impl;

import com.pagerealm.books.entity.Book;
import com.pagerealm.books.repository.BookRepository;
import com.pagerealm.shoppingcart.dto.request.AddItemToCartRequest;
import com.pagerealm.shoppingcart.dto.request.AddItemToWishfromCartRequest;
import com.pagerealm.shoppingcart.dto.request.RemoveItemFromCartRequest;
import com.pagerealm.shoppingcart.dto.response.CartItemResponse;
import com.pagerealm.shoppingcart.dto.response.CartResponse;
import com.pagerealm.shoppingcart.entity.*;
import com.pagerealm.shoppingcart.repository.*;
import com.pagerealm.shoppingcart.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {
    private final WishlistRepository wishlistRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AnonCartRedisRepository anonCartRedisRepository;
    private final BookRepository booksRepository;

    /**
     * 不建議使用 @Autowired 在屬性上進行注入，因為這樣會讓單元測試變得困難。
     * 使用建構子注入可以讓依賴關係更加明確，並且更容易進行單元測試。
     */
    public CartServiceImpl(WishlistRepository wishlistRepository,
                            CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           AnonCartRedisRepository anonCartRedisRepository,
                           BookRepository booksRepository) {
        this.wishlistRepository = wishlistRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.anonCartRedisRepository = anonCartRedisRepository;
        this.booksRepository = booksRepository;
    }

    /**
     * 合併匿名購物車到用戶購物車
     * @param anonCartId
     * @param userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeAnonCartToUserCart(String anonCartId, Long userId) {
        Set<String> anonBookIds = anonCartRedisRepository.getAll(anonCartId, "cart");
        if (anonBookIds == null || anonBookIds.isEmpty()) return;
        Cart userCart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setUserId(userId);
                    c.setStatus(CartStatus.ACTIVE);
                    c.setItems(new ArrayList<>());
                    return cartRepository.save(c);
                });
        // 取得用戶購物車中已存在的書籍 ID
        Set<Long> userBookIds = new HashSet<>();
        for (CartItem item : userCart.getItems()) {
            userBookIds.add(item.getBook().getId());
        }
        // 將匿名購物車中的書籍加入用戶購物車
        for (String bookIdStr : anonBookIds) {
            Long bookId = Long.valueOf(bookIdStr);
            if (!userBookIds.contains(bookId)) {
                Book book = booksRepository.findById(bookId).orElse(null);
                if (book != null) {
                    CartItem newItem = new CartItem();
                    newItem.setBook(book);
                    newItem.setCart(userCart);
                    newItem.setSnapshotPrice(book.getListPrice());
                    userCart.getItems().add(newItem);
                }
            }
        }
        cartRepository.save(userCart);
        anonCartRedisRepository.clear(anonCartId, "cart");
    }

    /**
     * 登入會員將商品加入購物車
     * @param userId
     * @param request
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItemToCart(Long userId, AddItemToCartRequest request) {
        if (request == null || request.getBookId() == null) {
            throw new IllegalArgumentException("商品資訊不正確");
        }
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setUserId(userId);
                    c.setStatus(CartStatus.ACTIVE);
                    c.setItems(new ArrayList<>());
                    return cartRepository.save(c);
                });
        boolean exists = cart.getItems().stream()
                .anyMatch(i -> i.getBook().getId().equals(request.getBookId()));
        if (!exists) {
            Book book = booksRepository.findById(request.getBookId()).orElseThrow();
            CartItem newItem = new CartItem();
            newItem.setBook(book);
            newItem.setCart(cart);
            newItem.setSnapshotPrice(book.getListPrice());
            cart.getItems().add(newItem);
            cartRepository.save(cart);
        }
    }

    /**
     * 取得用戶購物車
     * @param userId
     * @return
     */
    @Override
    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE).orElse(null);
        CartResponse response = new CartResponse();
        if (cart == null) {
            response.setCartId(null);
            response.setUserId(userId);
            response.setItems(Collections.emptyList());
            return response;
        }
        response.setCartId(cart.getId());
        response.setUserId(cart.getUserId());
        List<CartItemResponse> itemResponses = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            Book book = item.getBook();
            CartItemResponse itemResp = new CartItemResponse();
            itemResp.setBookId(book.getId());
            itemResp.setTitle(book.getTitle());
            itemResp.setAuthor(book.getAuthor());
            itemResp.setSnapshotPrice(item.getSnapshotPrice());
            itemResp.setFormat(book.getFormat());
            itemResp.setCoverImageUrl(book.getCoverImageUrl());
            itemResponses.add(itemResp);
        }
        response.setItems(itemResponses);
        return response;
    }

    /**
     * 根據用戶 ID 的請求刪除購物車中的商品
     * @param userId
     * @param request
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItemFromCart(Long userId, RemoveItemFromCartRequest request) {
        if (userId == null || request == null || request.getIds() == null|| request.getIds().isEmpty()) {
            throw new IllegalArgumentException("userId 或 cratItemIds 不可為空");
        }
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElse(null);
        if (cart == null) {
            throw new IllegalArgumentException("找不到該用戶的購物車");
        }
        List<Long> cratItemIds = request.getIds();
        List<CartItem> items = cart.getItems();
        // 使用 iterator 移除避免 ConcurrentModificationException
        // ConcurrentModificationException 是在使用增強型 for 迴圈或普通 for 迴圈時，嘗試在迴圈中修改集合（如刪除元素）所引發的異常。
        Iterator<CartItem> iterator = items.iterator();
        boolean removed = false;
        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            if (item.getId() != null && cratItemIds.contains(item.getId())) {
                iterator.remove();
                removed = true;
            }
        }
        if (!removed) {
            throw new IllegalArgumentException("購物車中未找到指定商品");
        }

        cartRepository.save(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItemToWishfromCart(Long userId, AddItemToWishfromCartRequest request){
        if (userId == null || request == null || request.getIds() == null|| request.getIds().isEmpty()) {
            throw new IllegalArgumentException("userId 或 cardIds 不可為空");
        }
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElse(null);
        if (cart == null) {
            throw new IllegalArgumentException("找不到該用戶的購物車");
        }
        Wishlist wishlist = wishlistRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseGet(() -> {
                    Wishlist w = new Wishlist();
                    w.setUserId(userId);
                    w.setStatus("ACTIVE");
                    w.setItems(new ArrayList<>());
                    return wishlistRepository.save(w);
                });

        List<Long> cartItemIds = request.getIds();
        List<CartItem> items = cart.getItems();
        Iterator<CartItem> iterator = items.iterator();
        boolean removed = false;
        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            if (item.getId() != null && cartItemIds.contains(item.getId())) {

                Book book = booksRepository.findById(item.getBook().getId()).orElseThrow();
                boolean alreadyInWishlist = wishlist.getItems().stream()
                        .anyMatch(wi -> wi.getBook().getId().equals(book.getId()));
                if (alreadyInWishlist) {
                    iterator.remove();  // 從購物車移除
                    removed = true;
                    continue;           // 不加入 wishlist
                }
                iterator.remove();
                WishlistItems newItem = new WishlistItems();
                newItem.setBook(book);
                newItem.setWishlist(wishlist);
                wishlist.getItems().add(newItem);
                wishlistRepository.save(wishlist);
                removed = true;
            }
        }
        if (!removed) {
            throw new IllegalArgumentException("購物車中未找到指定商品");
        }
        cartRepository.save(cart);
    }


}