package com.ehcache.service;





import com.ehcache.config.DynamicCacheService;
import com.ehcache.model.Product;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final DynamicCacheService cache;

    public ProductService(DynamicCacheService cache) {
        this.cache = cache;
    }

    public Product getProduct(String id) {
        Product cached = cache.get("productCache", id, String.class, Product.class,
                300, 100, 10, 20);

        if (cached != null) return cached;

        Product p = new Product(id, "Product-" + id);
        cache.put("productCache", id, p, String.class, Product.class,
                300, 100, 10, 20);

        return p;
    }
}
