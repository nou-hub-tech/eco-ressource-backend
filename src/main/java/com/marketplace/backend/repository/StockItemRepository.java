package com.marketplace.backend.repository;

import com.marketplace.backend.entity.StockItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {

  List<StockItem> findByProductIdProduct(Long idProduct);

  List<StockItem> findByCompanyId(Long companyId);

  List<StockItem> findByStatus(String status);
}
