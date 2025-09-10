package com.shop.shopping.shoppingcart.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Component
public class CartIdCookie {

    private static final int MAX_AGE_SECONDS = (int) Duration.ofDays(10).getSeconds();

    /**
     * 從請求讀取現有 cookie，有就回傳；沒有就產生一個新的 UUID
     * @param request
     * @param response
     * @return
     */
    public String getOrCreate(HttpServletRequest request, HttpServletResponse response,String cookieName) {
        String current = get(request, cookieName).orElse(null);
        if (current != null) return current;

        /* 產生一個全域唯一識別字串作為 cartId。 */
        String generated = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(cookieName, generated);
        cookie.setHttpOnly(true);
        /*正式環境再開*/
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(MAX_AGE_SECONDS);
        /*設定 SameSite=Lax，減少跨站請求時被送出的機會，沒上線沒差*/
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
        return generated;
    }

    /**
     * @description   從 request 的 cookie 陣列讀取 cartId 值
     * @param request
     * @return
     */
    public Optional<String> get(HttpServletRequest request,String cookieName) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(c -> c.getValue())
                .findFirst();
    }

    public void clear(HttpServletResponse response,String cookieName) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}