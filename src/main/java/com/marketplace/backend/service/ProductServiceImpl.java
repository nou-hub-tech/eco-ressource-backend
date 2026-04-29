package com.marketplace.backend.service;

import com.marketplace.backend.entity.Product;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.repository.IProductRepository;
import com.marketplace.backend.repository.IStockItemRepository;
import com.marketplace.backend.repository.IStockMovementRepository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements IProductService {

    private final IProductRepository productRepository;
    private final IStockItemRepository stockItemRepository;
    private final IStockMovementRepository stockMovementRepository;

    public ProductServiceImpl(
            IProductRepository productRepository,
            IStockItemRepository stockItemRepository,
            IStockMovementRepository stockMovementRepository
    ) {
        this.productRepository = productRepository;
        this.stockItemRepository = stockItemRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Override
    public List<Product> retrieveAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product addProduct(Product p) {
        return productRepository.save(p);
    }

    @Override
    public Product updateProduct(Product p) {
        return productRepository.save(p);
    }

    @Override
    public Product retrieveProduct(Long id_product) {
        return productRepository.findById(id_product).orElse(null);
    }

    @Override
    @Transactional
    public void removeProduct(Long id) {
        // 1. Get stock items for this product
        List<StockItem> stockItems = stockItemRepository.findByProductId(id);

        // 2. Delete movements first
        for (StockItem si : stockItems) {
            stockMovementRepository.deleteByStockItem_IdStock(si.getIdStock()); // ✅ FIXED
        }

        // 3. Delete stock items
        stockItemRepository.deleteAll(stockItems);

        // 4. Delete product
        productRepository.deleteById(id);
    }

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