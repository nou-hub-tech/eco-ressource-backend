package com.marketplace.backend.repository;

import com.marketplace.backend.entity.ExchangeRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {

  List<ExchangeRequest> findByRecipientEnterpriseId(Long enterpriseId);
}
