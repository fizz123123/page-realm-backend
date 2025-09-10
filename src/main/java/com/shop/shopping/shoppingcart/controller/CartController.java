package com.shop.shopping.shoppingcart.controller;

import com.shop.shopping.pagerealm.security.service.UserDetailsImpl;
import com.shop.shopping.shoppingcart.cookie.CartIdCookie;
import com.shop.shopping.shoppingcart.dto.request.AddItemToAnonCartRequest;
import com.shop.shopping.shoppingcart.dto.request.AddItemToCartRequest;
import com.shop.shopping.shoppingcart.dto.request.AddItemToWishfromCartRequest;
import com.shop.shopping.shoppingcart.dto.request.RemoveItemFromCartRequest;
import com.shop.shopping.shoppingcart.dto.response.CartResponse;
import com.shop.shopping.shoppingcart.repository.AnonCartRedisRepository;
import com.shop.shopping.shoppingcart.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/cart")
public class CartController {
    private final AnonCartRedisRepository anonCartRepo;
    private final CartIdCookie cartIdCookie;
    private final CartService cartService;

    public CartController(AnonCartRedisRepository anonCartRepo, CartIdCookie cartIdCookie, CartService cartService) {
        this.anonCartRepo = anonCartRepo;
        this.cartIdCookie = cartIdCookie;
        this.cartService = cartService;
    }

    /**
     * 未登入購物車
     * @param
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/public/add")
    public ResponseEntity<?> addItem( @RequestBody AddItemToAnonCartRequest cartrequest,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        String cartId = cartIdCookie.getOrCreate(request, response,"cart");
        anonCartRepo.addItem(cartId,"cart" ,cartrequest);

        return ResponseEntity.ok(Map.of("status", "ok", "booksId", cartrequest.getBookId()));
    }

    /**
     * 登入後加入購物車
     * @param request
     * @param userDetails
     * @return
     */
    @PostMapping("/add")
    public ResponseEntity<?> addItemToUserCart(@AuthenticationPrincipal UserDetailsImpl userDetails,@RequestBody AddItemToCartRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("未登入");
        }
        cartService.addItemToCart(userDetails.getId(), request);
        return ResponseEntity.ok(Map.of("status", "ok", "bookId", request.getBookId()));
    }

    /**
     * 查看用戶購物車
     * @param userDetails
     * @return
     */
    @GetMapping("/view")
    public ResponseEntity<?> getUserCart(@AuthenticationPrincipal UserDetailsImpl userDetails){
        if (userDetails == null) {
            return ResponseEntity.status(401).body("未登入");
        }
        CartResponse response = cartService.getCart(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * 從購物車移除商品
     * @param request
     * @param userDetails
     * @return
     */
    @DeleteMapping("/remove")
    public ResponseEntity<?> removeItemFromCart(@RequestBody RemoveItemFromCartRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails){
        if (userDetails == null) {
            return ResponseEntity.status(401).body("未登入");
        }
        cartService.removeItemFromCart(userDetails.getId(), request);
        return ResponseEntity.ok(Map.of("status", "ok", "cartItemId", request.getIds()));
    }

    /**
     * 購物車商品轉移願望清單
     * @param request
     * @param userDetails
     * @return
     */
    @PostMapping("/toWish")
    public  ResponseEntity<?> toWish(@RequestBody AddItemToWishfromCartRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("未登入");
        }
        cartService.addItemToWishfromCart(userDetails.getId(), request);
        return ResponseEntity.ok(Map.of("status", "ok", "cartItemId", request.getIds()));
    }

}
