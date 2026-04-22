package com.marketplace.backend.repository;

import com.marketplace.backend.entity.InventoryScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IInventoryScanRepository extends JpaRepository<InventoryScan, Long> {
    List<InventoryScan> findByBarcodeOrderByScannedAtDesc(String barcode);
    List<InventoryScan> findAllByOrderByScannedAtDesc();
}