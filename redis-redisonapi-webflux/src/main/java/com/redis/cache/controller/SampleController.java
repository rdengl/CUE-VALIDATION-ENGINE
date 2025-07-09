package com.redis.cache.controller;

import com.redis.cache.model.User;
import com.redis.cache.service.ReactiveCacheService;
import com.redis.cache.service.ReactiveCacheServiceSpring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/users")
public class SampleController {
    //private final ReactiveCacheServiceSpring cacheService;

    @Autowired
    private  ReactiveCacheService service;



    @GetMapping("/{id}")
    public Mono<User> getUser(@PathVariable String id) {
        return service.get("user:" + id, User.class)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found in cache")));
    }

    @PostMapping("/{id}")
    public Mono<Boolean> saveUser(@PathVariable String id, @RequestBody User user) {
        return service.put("user:" + id, user, Duration.ofMinutes(1));
    }
}