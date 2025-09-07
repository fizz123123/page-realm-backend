package com.pagerealm.shoppingcart.controller;

import com.pagerealm.authentication.security.service.UserDetailsImpl;
import com.pagerealm.shoppingcart.cookie.CartIdCookie;
import com.pagerealm.shoppingcart.dto.request.AddItemToAnonWishRequest;
import com.pagerealm.shoppingcart.dto.request.AddItemToCartfromWishRequest;
import com.pagerealm.shoppingcart.dto.request.AddItemToWishRequest;
import com.pagerealm.shoppingcart.dto.request.RemoveItemFromWishRequest;
import com.pagerealm.shoppingcart.dto.response.WishResponse;
import com.pagerealm.shoppingcart.repository.AnonWishlistRedisRepository;
import com.pagerealm.shoppingcart.service.WishlistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/wish")
public class WishlistController {
    private final AnonWishlistRedisRepository anonWishlistRepo;
    private final CartIdCookie cartIdCookie;
    private final WishlistService wishlistService;

    public WishlistController(AnonWishlistRedisRepository anonWishlistRepo,
                              CartIdCookie cartIdCookie,
                              WishlistService wishlistService)
    {
        this.anonWishlistRepo = anonWishlistRepo;
        this.cartIdCookie = cartIdCookie;
        this.wishlistService = wishlistService;
    }

    /**
     * 未登入願望清單
     * @param
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/public/add")
    public ResponseEntity<?> addItem(@RequestBody AddItemToAnonWishRequest wishrequest,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        String wishlistId = cartIdCookie.getOrCreate(request, response,"wish");
        anonWishlistRepo.addItem(wishlistId,"wishlist" ,wishrequest);

        return ResponseEntity.ok(Map.of("status", "ok", "booksId", wishrequest.getBookId()));
    }

    /**
     * 加入商品至願望清單
     * @param userDetails
     * @param request
     * @return
     */
    @PostMapping("/add")
    public ResponseEntity<?> addItemToUserWish(@AuthenticationPrincipal UserDetailsImpl userDetails,@RequestBody AddItemToWishRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("未登入");
        }
        wishlistService.addItemToWish(userDetails.getId(), request);
        return ResponseEntity.ok(Map.of("status", "ok", "bookId", request.getBookId()));
    }

    /**
     * 查看用戶願望清單
     * @param userDetails
     * @return
     */
    @GetMapping("/view")
    public ResponseEntity<?> getUserWish(@AuthenticationPrincipal UserDetailsImpl userDetails){
        if (userDetails == null) {
            return ResponseEntity.status(401).body("未登入");
        }
        try {
            WishResponse response = wishlistService.getWish(userDetails.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * 從願望清單移除商品
     * @param request
     * @param userDetails
     * @return
     */
    @DeleteMapping("/remove")
    public ResponseEntity<?> removeItemFromWish(@RequestBody RemoveItemFromWishRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails){
        if (userDetails == null) {
            return ResponseEntity.status(401).body("未登入");
        }
        wishlistService.removeItemFromWish(userDetails.getId(), request);
        return ResponseEntity.ok(Map.of("status", "ok", "wishItemId", request.getIds()));
    }

    /**
     * 願望清單商品轉移購物車
     * @param request
     * @param userDetails
     * @return
     */
    @PostMapping("/toCart")
    public  ResponseEntity<?> toWish(@RequestBody AddItemToCartfromWishRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("未登入");
        }
        wishlistService.addItemToCartfromWish(userDetails.getId(), request);
        return ResponseEntity.ok(Map.of("status", "ok", "wishItemId", request.getIds()));
    }

}