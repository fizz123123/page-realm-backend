package com.shop.shopping.shoppingcart.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@EnableCaching
@Configuration
public class RedisConfig {

    // 給購物車 Set 用：Key 與 Value 都是 String（例如 key: cart:anon:uuid, value: productId）

    /**
     * 設定Key/Value儲存序列化
     * @param connectionFactory
     * @return :
     */
    @Bean
    public RedisTemplate<String, String> stringSetRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }

    //

    /**
     * Jackson - JSON 處理庫的核心類別暫時用不到
     * @return : 1
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return om;
    }

    // Spring Cache 的 CacheManager：用來 @Cacheable 商品資訊等（非購物車 Set 操作）暫時用不到

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper());

        // 預設快取存活時間 12 小時，可依需求調整
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(12))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .computePrefixWith(cacheName -> "cache:" + cacheName + ":");

        // 針對不同 cache 名稱客製 TTL
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        // 商品資訊 24 小時
        cacheConfigs.put("product", defaultConfig.entryTtl(Duration.ofHours(24)));
        // 會員購物車快取 1 小時（暫時用不到）
        cacheConfigs.put("memberCart", defaultConfig.entryTtl(Duration.ofHours(1)));
        // 匿名購物車 meta 10 天
        cacheConfigs.put("anonCartMeta", defaultConfig.entryTtl(Duration.ofDays(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }


}