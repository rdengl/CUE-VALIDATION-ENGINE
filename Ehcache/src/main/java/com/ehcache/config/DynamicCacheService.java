package com.ehcache.config;




import com.ehcache.config.CacheProperties;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.*;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DynamicCacheService {

    private static final Logger log = LoggerFactory.getLogger(DynamicCacheService.class);

    private final CacheManager cacheManager;
    private final CacheProperties properties;
    private final ConcurrentHashMap<String, Cache<?, ?>> registry = new ConcurrentHashMap<>();

    public DynamicCacheService(CacheManager cacheManager, CacheProperties properties) {
        this.cacheManager = cacheManager;
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateCache(
            String cacheName,
            Class<K> keyType,
            Class<V> valueType,
            long ttlSeconds,
            long heapEntries,
            long offheapMB,
            long diskMB
    ) {
        return (Cache<K, V>) registry.computeIfAbsent(cacheName, name -> {
            log.info("Creating cache '{}': TTL={}s HEAP={} OFFHEAP={}MB {}",
                    cacheName, ttlSeconds, heapEntries, offheapMB,
                    properties.isDiskEnabled() ? "DISK=" + diskMB + "MB" : "DISK=DISABLED");

            ResourcePoolsBuilder pool = ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(heapEntries, EntryUnit.ENTRIES)
                    .offheap(offheapMB, MemoryUnit.MB);

            if (properties.isDiskEnabled()) {
                pool = pool.disk(diskMB, MemoryUnit.MB, true);
            }

            CacheConfiguration<K, V> config = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                            keyType, valueType, pool)
                    .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ttlSeconds)))
                    .build();

            return cacheManager.createCache(cacheName, config);
        });
    }

    public <K, V> void put(String cacheName, K key, V value,
                           Class<K> keyType, Class<V> valueType,
                           long ttl, long heap, long offheap, long disk) {
        Cache<K, V> cache = getOrCreateCache(cacheName, keyType, valueType, ttl, heap, offheap, disk);
        cache.put(key, value);
        log.info("✅ PUT [{}] => [{}] into cache '{}'", key, value, cacheName);
    }

    public <K, V> V get(String cacheName, K key,
                        Class<K> keyType, Class<V> valueType,
                        long ttl, long heap, long offheap, long disk) {
        Cache<K, V> cache = getOrCreateCache(cacheName, keyType, valueType, ttl, heap, offheap, disk);
        V value = cache.get(key);
        log.info(value != null ?
                "✅ GET [{}] from cache '{}': HIT" :
                "❌ GET [{}] from cache '{}': MISS", key, cacheName);
        return value;
    }
}

