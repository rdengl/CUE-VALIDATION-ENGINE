package com.ehcache.config;


import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.impl.config.persistence.CacheManagerPersistenceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class EhcacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CacheManagerBuilder<?> builder = CacheManagerBuilder.newCacheManagerBuilder();

        if (false) {
            builder = CacheManagerBuilder.newCacheManagerBuilder()
                    .with(new CacheManagerPersistenceConfiguration(new File("ehcache-data")));
        }

        return builder.build(true);
    }
}

