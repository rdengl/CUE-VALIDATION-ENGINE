package com.redis.cache.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
public class ReactiveCacheService {

    private static final Logger log = LoggerFactory.getLogger(ReactiveCacheService.class);

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ReactiveHashOperations<String, String, Object> hashOperations;
    private final ObjectMapper objectMapper; // Used for converting objects to/from Map for Hashes

    public ReactiveCacheService(ReactiveRedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
        this.objectMapper = objectMapper;
    }

    /**
     * A generic method to implement the "cache-aside" pattern.
     * It tries to get a value from the cache. If the value is not present (cache miss),
     * it executes the fallback Mono, puts its result into the cache, and then returns it.
     *
     * @param key      The cache key.
     * @param fallback A Mono that provides the value if the cache is missed. This is typically a database or API call.
     * @param ttl      The Time-To-Live for the new cache entry.
     * @param type     The expected class of the object.
     * @param <T>      The type of the object.
     * @return A Mono emitting the cached or newly fetched value.
     */
    public <T> Mono<T> getOrSet(String key, Mono<T> fallback, Duration ttl, Class<T> type) {
        return get(key, type)
                .switchIfEmpty(
                        Mono.defer(() -> {
                            log.info("Cache MISS for key: {}. Executing fallback.", key);
                            return fallback.flatMap(value -> put(key, value, ttl).thenReturn(value));
                        })
                );
    }


    // --- Standard String/Value Operations ---

    public <T> Mono<T> get(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key)
                .doOnNext(v -> log.trace("Cache HIT for key: {}", key))
                .cast(clazz);
    }

    public <T> Mono<Boolean> put(String key, T value, Duration ttl) {
        log.trace("Populating cache for key: {} with TTL: {}", key, ttl);
        return redisTemplate.opsForValue().set(key, value, ttl);
    }

    public Mono<Boolean> delete(String key) {
        return redisTemplate.opsForValue().delete(key);
    }


    // --- Generic Hash Operations ---

    /**
     * Retrieves an entire object stored as a Hash in Redis.
     *
     * @param key  The key of the hash.
     * @param type The class to convert the hash into.
     * @param <T>  The type of the object.
     * @return A Mono emitting the deserialized object or empty if the key doesn't exist.
     */
    public <T> Mono<T> hGetAll(String key, Class<T> type) {
        return hashOperations.entries(key)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .filter(map -> !map.isEmpty())
                .map(map -> objectMapper.convertValue(map, type))
                .doOnNext(v -> log.trace("Cache HIT for hash key: {}", key));
    }

    /**
     * Stores an entire object as a Hash in Redis.
     *
     * @param key   The key for the hash.
     * @param value The object to store. It will be converted to a Map.
     * @param ttl   The Time-To-Live for the hash.
     * @return A Mono emitting true if the operation was successful.
     */
    public Mono<Boolean> hSetAll(String key, Object value, Duration ttl) {
        log.trace("Populating cache for hash key: {} with TTL: {}", key, ttl);
        Map<String, Object> map = objectMapper.convertValue(value, Map.class);
        return hashOperations.putAll(key, map)
                .then(redisTemplate.expire(key, ttl));
    }
}