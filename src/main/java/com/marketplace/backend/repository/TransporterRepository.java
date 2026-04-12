package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Transporter;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransporterRepository extends JpaRepository<Transporter, Long> {

  Optional<Transporter> findByUserId(Long userId);
}
