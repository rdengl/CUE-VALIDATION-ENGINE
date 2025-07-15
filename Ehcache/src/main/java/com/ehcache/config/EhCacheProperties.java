package com.ehcache.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ehcache")
public class EhCacheProperties {

    private Map<String, CacheSpec> caches = new HashMap<>();

    public Map<String, CacheSpec> getCaches() {
        return caches;
    }

    public void setCaches(Map<String, CacheSpec> caches) {
        this.caches = caches;
    }

    public static class CacheSpec {
        private long ttl;
        private long heap;
        private long diskSize;
        private boolean diskPersistent;

        public long getTtl() { return ttl; }
        public void setTtl(long ttl) { this.ttl = ttl; }

        public long getHeap() { return heap; }
        public void setHeap(long heap) { this.heap = heap; }

        public long getDiskSize() { return diskSize; }
        public void setDiskSize(long diskSize) { this.diskSize = diskSize; }

        public boolean isDiskPersistent() { return diskPersistent; }
        public void setDiskPersistent(boolean diskPersistent) {
            this.diskPersistent = diskPersistent;
        }
    }
}
