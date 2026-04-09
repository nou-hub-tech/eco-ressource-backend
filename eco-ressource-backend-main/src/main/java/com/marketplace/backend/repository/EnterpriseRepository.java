package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Enterprise;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnterpriseRepository extends JpaRepository<Enterprise, Long> {

  Optional<Enterprise> findByUserId(Long userId);
}
