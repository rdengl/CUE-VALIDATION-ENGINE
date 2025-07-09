package com.redis.cache.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucketReactive;
import org.redisson.api.RedissonReactiveClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;


@Service
public class ReactiveCacheServiceSpring {
    private static final Logger log = LoggerFactory.getLogger(ReactiveCacheServiceSpring.class);

    private final RedissonReactiveClient redisson;
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    private final ObjectMapper objectMapper;

    public ReactiveCacheServiceSpring(RedissonReactiveClient redisson) {
        this.redisson = redisson;
        this.objectMapper = new ObjectMapper();
    }

    // ---------- MONO (single object) ----------
    public <T> Mono<T> getOrSet(String key, Mono<T> fallback, Duration ttl, Class<T> type) {
        RBucketReactive<String> bucket = redisson.getBucket(key);
        return bucket.get()
                .flatMap(json -> deserializeMono(json, type))
                .switchIfEmpty(fallback
                        .flatMap(value ->
                                serializeMono(value)
                                        .flatMap(serialized -> bucket.set(serialized, ttl != null ? ttl : DEFAULT_TTL))
                                        .thenReturn(value)
                        )
                );
    }

    // ---------- FLUX (list of objects) ----------
    public <T> Flux<T> getOrSetFlux(String key, Flux<T> fallback, Duration ttl, Class<T> elementType) {
        RBucketReactive<String> bucket = redisson.getBucket(key);
        return bucket.get()
                .flatMapMany(json -> deserializeFlux(json, elementType))
                .switchIfEmpty(fallback
                        .collectList()
                        .flatMap(list -> serializeMono(list)
                                .flatMap(serialized -> bucket.set(serialized, ttl != null ? ttl : DEFAULT_TTL))
                                .thenReturn(list)
                        )
                        .flatMapMany(Flux::fromIterable)
                );
    }

    // ---------- SERIALIZATION ----------
    private <T> Mono<String> serializeMono(T value) {
        try {
            return Mono.just(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            return Mono.error(new SerializationException("Serialization failed", e));
        }
    }

    // ---------- DESERIALIZATION (Mono) ----------
    private <T> Mono<T> deserializeMono(String json, Class<T> type) {
        try {
            return Mono.just(objectMapper.readValue(json, type));
        } catch (Exception e) {
            return Mono.error(new SerializationException("Deserialization failed", e));
        }
    }

    // ---------- DESERIALIZATION (Flux from List<T>) ----------
    private <T> Flux<T> deserializeFlux(String json, Class<T> elementType) {
        try {
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            List<T> list = objectMapper.readValue(json, listType);
            return Flux.fromIterable(list);
        } catch (Exception e) {
            return Flux.error(new SerializationException("Deserialization failed", e));
        }
    }

    // ---------- GET (single object) ----------
    public <T> Mono<T> get(String key, Class<T> type) {
        log.trace("Attempting to get key: {}", key);
        return redisson.getBucket(key)
                .get() // Returns Mono<String> with the raw JSON
                .doOnSuccess(json -> {
                    if (json != null) {
                        log.trace("Cache HIT for key: {}", key);
                    } else {
                        log.trace("Cache MISS for key: {}", key);
                    }
                })
                .flatMap(json -> deserializeMono((String) json, type));
    }

    // ---------- SET (single object) ----------
    public <T> Mono<Boolean> set(String key, T value, Duration ttl) {
        log.trace("Setting cache for key: {} with TTL: {}", key, ttl);
        return serializeMono(value)
                .flatMap(serializedValue ->
                        redisson.getBucket(key).set(serializedValue, ttl != null ? ttl : DEFAULT_TTL)
                )
                .thenReturn(true) // If the above completes successfully, return true
                .onErrorReturn(false); // If any error occurs (e.g., serialization), return false
    }

    // ---------- DELETE ----------
    public Mono<Boolean> delete(String key) {
        log.trace("Deleting key: {}", key);
        return redisson.getBucket(key).delete(); // RBucket.delete() already returns Mono<Boolean>
    }

    // ---------- DELETE ----------
    public Mono<Object> expireKey(String key) {
        log.trace("expireKey key: {}", key);
        return redisson.getBucket(key).get(); // RBucket.delete() already returns Mono<Boolean>
    }

}

