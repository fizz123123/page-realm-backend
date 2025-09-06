package com.pagerealm.shoppingcart.repository;

import com.pagerealm.shoppingcart.dto.request.AddItemToAnonCartRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@Repository
public class AnonCartRedisRepository {
    private final RedisTemplate<String, String> redis;
    private static final Duration ANON_CART_TTL = Duration.ofDays(10);

    public AnonCartRedisRepository(RedisTemplate<String, String> stringSetRedisTemplate) {
        this.redis = stringSetRedisTemplate;
    }

    private String key(String keyType,String cartId) {
        return "anon:"+keyType+ ":" + cartId;
    }

    /**
     * 添加新商品，商品不重複。
     * 每次修改會增加到期時間
     * @param cartId
     * @param
     */
    public void addItem(String cartId, String keyType, AddItemToAnonCartRequest request) {
        String k = key(keyType, cartId);
        redis.opsForSet().add(k, String.valueOf(request.getBookId()));
        redis.expire(k, ANON_CART_TTL);
    }

    /**
     * 從Set移除商品
     * @param cartId:
     * @param booksId:
     */
    public void removeItem(String cartId, String keyType, String booksId) {
        String k = key(keyType,cartId);
        redis.opsForSet().remove(k, booksId);
        redis.expire(k, ANON_CART_TTL);
    }

    /**
     * 取全Set
     * @param cartId:
     * @return :
     */
    public Set<String> getAll(String cartId, String keyType) {
        String k = key(keyType,cartId);
        Set<String> members = redis.opsForSet().members(k);
        return members != null ? members : Collections.emptySet();
    }

    /**
     * 查商品是否在Set內
     * @param cartId:
     * @param booksId:
     * @return :
     */
    public boolean contains(String cartId, String keyType, String booksId) {
        String k = key(keyType,cartId);
        Boolean exists = redis.opsForSet().isMember(k, booksId);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Set大小
     * @param cartId:
     * @return :
     */
    public long size(String cartId, String keyType) {
        String k = key(keyType,cartId);
        Long size = redis.opsForSet().size(k);
        return size != null ? size : 0;
    }

    /**
     * 刪掉整個匿名購物車
     * @param cartId:
     */
    public void clear(String cartId, String keyType) {
        String k = key(keyType,cartId);
        redis.delete(k);
    }


}