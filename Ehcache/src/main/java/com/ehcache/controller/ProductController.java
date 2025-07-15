package com.ehcache.controller;




import com.ehcache.model.Product;
import com.ehcache.service.ProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable String id) {
        return service.getProduct(id);
    }
}


