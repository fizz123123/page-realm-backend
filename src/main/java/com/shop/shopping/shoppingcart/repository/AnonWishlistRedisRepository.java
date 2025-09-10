package com.shop.shopping.shoppingcart.repository;

import com.shop.shopping.shoppingcart.dto.request.AddItemToAnonWishRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@Repository
public class AnonWishlistRedisRepository {
    private final RedisTemplate<String, String> redis;
    private static final Duration ANON_CART_TTL = Duration.ofDays(10);

    public AnonWishlistRedisRepository(RedisTemplate<String, String> stringSetRedisTemplate) {
        this.redis = stringSetRedisTemplate;
    }

    private String key(String keyType,String wishlistId) {
        return "anon:"+keyType+ ":" + wishlistId;
    }

    /**
     * 添加新商品
     * @param wishlistId
     * @param
     */
    public void addItem(String wishlistId, String keyType, AddItemToAnonWishRequest request) {
        String k = key(keyType, wishlistId);
        redis.opsForSet().add(k, String.valueOf(request.getBookId()));
        redis.expire(k, ANON_CART_TTL);
    }

    /**
     * 取全Set
     * @param wishlistId:
     * @return :
     */
    public Set<String> getAll(String wishlistId, String keyType) {
        String k = key(keyType,wishlistId);
        Set<String> members = redis.opsForSet().members(k);
        return members != null ? members : Collections.emptySet();
    }

    /**
     * Set大小
     * @param wishlistId:
     * @return :
     */
    public long size(String wishlistId, String keyType) {
        String k = key(keyType,wishlistId);
        Long size = redis.opsForSet().size(k);
        return size != null ? size : 0;
    }

    /**
     * 刪掉整個匿名購物車
     * @param wishlistId:
     */
    public void clear(String wishlistId, String keyType) {
        String k = key(keyType,wishlistId);
        redis.delete(k);
    }

}
