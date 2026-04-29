package com.marketplace.backend.repository;

import com.marketplace.backend.entity.EcoOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EcoOrderRepository extends JpaRepository<EcoOrder, Long> {

  List<EcoOrder> findByEnterpriseId(Long enterpriseId);

  @Query(
      "SELECT o FROM EcoOrder o "
          + "WHERE o.enterprise.id = :eid "
          + "AND (o.deleted IS NULL OR o.deleted = false)")
  List<EcoOrder> findActiveByEnterpriseId(@Param("eid") Long eid);

  @Query("SELECT o FROM EcoOrder o WHERE (o.deleted IS NULL OR o.deleted = false)")
  List<EcoOrder> findAllActive();

  Optional<EcoOrder> findByRef(String ref);
}
