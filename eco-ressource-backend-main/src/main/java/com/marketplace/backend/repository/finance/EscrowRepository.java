package com.marketplace.backend.repository.finance;

import com.marketplace.backend.entity.finance.escrow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscrowRepository extends JpaRepository<escrow, Long> {

    List<escrow> findByEnterpriseId(Long enterpriseId);
}
