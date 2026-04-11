package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Delivery;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

  List<Delivery> findByEnterpriseId(Long enterpriseId);

  List<Delivery> findByTransporterId(Long transporterId);
}
