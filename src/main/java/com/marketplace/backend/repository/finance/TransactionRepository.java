package com.marketplace.backend.repository.finance;

import com.marketplace.backend.entity.finance.transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<transaction, Long> {

    /** Transactions de l'entreprise connectee */
    List<transaction> findByEnterpriseId(Long enterpriseId);
}
