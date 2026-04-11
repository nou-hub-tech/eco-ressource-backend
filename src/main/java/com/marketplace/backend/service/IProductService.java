package com.marketplace.backend.service;

import com.marketplace.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IProductService {
    List<Product> retrieveAllProducts();
    Page<Product> retrieveAllProducts(Pageable pageable);
    Product addProduct(Product p);
    Product updateProduct(Product p);
    Product retrieveProduct(Long id_product);
    void removeProduct(Long id_product);
    List<Product> searchProducts(String name, String category, String materialType);
}