package com.shop.shopping.shoppingcart.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

public class CheckMacValueUtil {

    public static String generate(Map<String, String> params, String hashKey, String hashIv) {
        Map<String, String> filtered = params.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null && !"CheckMacValue".equalsIgnoreCase(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        List<String> keys = new ArrayList<>(filtered.keySet());
        Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
        String query = keys.stream()
                .map(k -> k + "=" + filtered.get(k))
                .collect(Collectors.joining("&"));
        String raw = "HashKey=" + hashKey + "&" + query + "&HashIV=" + hashIv;
        String encoded = urlEncodeForEcpay(raw).toLowerCase(Locale.ROOT);

        return sha256Hex(encoded).toUpperCase(Locale.ROOT);
    }

    private static String urlEncodeForEcpay(String value) {
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("%2d", "–")
                .replace("%5f", "_")
                .replace("%2e", ".")
                .replace("%21", "!")
                .replace("%2a", "*")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%20", "+");
        return encoded;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }
}
