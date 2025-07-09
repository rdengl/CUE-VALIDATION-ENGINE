package com.redis.cache.config;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;

@Configuration
public class RedisConfig {

    @Bean
    public RedissonClient redissonClient(Config config) {
        // You can use RedissonClient to interact with Redis
        // For example: RMap<String, String> myMap = redissonClient.getMap("myMap");
        // myMap.put("key", "value");

        return Redisson.create(config);
    }

    @Bean
    public Config redissonConfig(@Value("${redisson.config}") String config) throws IOException {
        return Config.fromYAML(config);
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        RedisSerializationContext<String, Object> context = RedisSerializationContext
                .<String, Object>newSerializationContext(new StringRedisSerializer())
                .value(new GenericJackson2JsonRedisSerializer())
                .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }



}