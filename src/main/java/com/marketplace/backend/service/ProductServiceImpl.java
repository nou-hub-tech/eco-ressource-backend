package com.marketplace.backend.service;

import com.marketplace.backend.entity.Product;
import com.marketplace.backend.repository.IProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements IProductService {

    private final IProductRepository productRepository;

    public ProductServiceImpl(IProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> retrieveAllProducts() { return productRepository.findAll(); }

    @Override
    public Product addProduct(Product p) { return productRepository.save(p); }

    @Override
    public Product updateProduct(Product p) { return productRepository.save(p); }

    @Override
    public Product retrieveProduct(Long id_product) {
        return productRepository.findById(id_product).orElse(null);
    }

    @Override
    public void removeProduct(Long id_product) { productRepository.deleteById(id_product); }

    @Override
    public List<Product> searchProducts(String name, String category, String materialType) {
        return productRepository.searchProducts(
                (name != null && !name.isEmpty()) ? name : null,
                (category != null && !category.isEmpty()) ? category : null,
                (materialType != null && !materialType.isEmpty()) ? materialType : null
        );
    }
    @Override
    public Page<Product> retrieveAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }
}