package com.marketplace.backend.repository.finance;

import com.marketplace.backend.entity.finance.FinancingRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancingRequestRepository extends JpaRepository<FinancingRequest, Long> {
}
