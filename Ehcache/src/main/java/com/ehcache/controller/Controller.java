package com.ehcache.controller;

import com.ehcache.config.DynamicCacheService;
import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private static final Logger log = LoggerFactory.getLogger(CacheController.class);

    private final DynamicCacheService cacheService;

    public CacheController(DynamicCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * GET value by cache name and key
     */
    @GetMapping("/{cacheName}/{key}")
    public ResponseEntity<?> getCacheValue(
            @PathVariable String cacheName,
            @PathVariable String key
    ) {
        Object value = cacheService.get(cacheName, key, String.class, Object.class,
                60, 100, 50, 100); // TTL and config defaults
        return value != null
                ? ResponseEntity.ok(value)
                : ResponseEntity.notFound().build();
    }

    /**
     * DELETE a specific cache entry
     */
    @DeleteMapping("/{cacheName}/{key}")
    public ResponseEntity<?> deleteCacheKey(
            @PathVariable String cacheName,
            @PathVariable String key
    ) {
        cacheService.evict(cacheName, key);
        return ResponseEntity.ok("Key deleted from cache: " + key);
    }

    /**
     * POST to clear all entries in a cache (not deleting the cache)
     */
    @PostMapping("/{cacheName}/clear")
    public ResponseEntity<?> clearCache(@PathVariable String cacheName) {
        cacheService.clearCache(cacheName);
        return ResponseEntity.ok("Cache cleared: " + cacheName);
    }

    /**
     * DELETE the entire cache (destroy)
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<?> deleteCache(@PathVariable String cacheName) {
        cacheService.removeCache(cacheName);
        return ResponseEntity.ok("Cache removed: " + cacheName);
    }
}
