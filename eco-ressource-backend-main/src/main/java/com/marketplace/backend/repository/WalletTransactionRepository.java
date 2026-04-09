package com.marketplace.backend.repository;

import com.marketplace.backend.entity.WalletTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

  List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
