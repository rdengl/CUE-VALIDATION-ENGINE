package com.redis.cache.controller;

import com.redis.cache.model.User;
import com.redis.cache.service.ReactiveCacheService;
import com.redis.cache.service.ReactiveCacheServiceSpring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/reactive/user")
public class UserReactiveController {

    private static final Logger log = LoggerFactory.getLogger(UserReactiveController.class);
    private final ReactiveCacheService cacheService;

    @Autowired
    ReactiveCacheServiceSpring reactiveCacheServiceSpring;

    public UserReactiveController(ReactiveCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/{id}")
    public Mono<User> getUserWithMono(@PathVariable String id) {
        Mono<User> userMono = reactiveCacheServiceSpring.get("user:"+id, User.class);
        return userMono;
        // return cacheService.getOrSet("user:" + id,  simulateDbCall(id),Duration.ofMillis(19000), User.class );
    }

   // @GetMapping("/{id}")
    public Mono<User> getUserWithTTL(@PathVariable String id) {
        Mono<User> userMono = reactiveCacheServiceSpring.getOrSet(
                "user:101",
                simulateDbCall(id),  // fallback
                //Duration.ofMinutes(1),
                Duration.ofMillis(19000),
                User.class
        );
        return userMono;
        // return cacheService.getOrSet("user:" + id,  simulateDbCall(id),Duration.ofMillis(19000), User.class );
    }

    @GetMapping("/all")
    public Flux<User> getAllUsers() {
        Flux<User> users = reactiveCacheServiceSpring.getOrSetFlux(
                "users:active",
                simulateDbFetchAll(), // fallback
                Duration.ofMillis(19000),
                User.class
        );
        return users;
        //return cacheService.getOrSet("users:all", User.class, this::simulateDbFetchAll);
    }

    /**
     * Sets or updates a value in the cache for a given key.
     *
     * @param user          The key for the cache entry, provided in the URL path.
     *                     Note: For production, it's better to use a specific DTO class instead of Object.
     * @param ttlInSeconds The time-to-live for the cache entry in seconds (optional).
     *                     If not provided, the service's default TTL will be used.
     * @return A Mono with an HTTP 200 (OK) on success, or HTTP 500 (Internal Server Error) on failure.
     */
    @PutMapping("/{key}")
    public Mono<ResponseEntity<Void>> setCache(@RequestBody User user,
            @RequestParam(name = "ttl", required = false) Long ttlInSeconds) {

        log.info("Request to set cache for key: {}", user.getId());
        Duration ttl = (ttlInSeconds != null) ? Duration.ofSeconds(ttlInSeconds) : null;

        return reactiveCacheServiceSpring.set("user:"+user.getId(), user, ttl)
                .map(success -> success ?
                        ResponseEntity.ok().<Void>build() :
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build()
                );
    }

    /**
     * Deletes an entry from the cache by its key.
     *
     * @param key The key of the entry to delete.
     * @return A Mono with an HTTP 204 (No Content) if the key was found and deleted,
     *         or an HTTP 404 (Not Found) if the key did not exist.
     */
    @DeleteMapping("/{key}")
    public Mono<ResponseEntity<Void>> deleteCache(@PathVariable String key) {
        log.info("Request to delete cache for key: {}", key);
        return cacheService.delete(key)
                .map(deleted -> deleted ?
                        ResponseEntity.noContent().build() :
                        ResponseEntity.notFound().build()
                );
    }


    private Mono<User> simulateDbCall(String id) {
        log.info("Simulating DB call for id={}", id);
        return Mono.just(new User(id, "Ram", 18)).delayElement(Duration.ofMillis(12000));
    }

    private Flux<User> simulateDbFetchAll() {
        log.info("Simulating DB fetch all");
        return Flux.just(
                new User("1", "User-1", 20),
                new User("2", "User-2", 30)
        ).delayElements(Duration.ofMillis(12000));
    }
}
