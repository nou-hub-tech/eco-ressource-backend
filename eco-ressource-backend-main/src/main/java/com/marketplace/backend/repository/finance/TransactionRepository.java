package com.marketplace.backend.repository.finance;

import com.marketplace.backend.entity.finance.transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<transaction, Long> {}
