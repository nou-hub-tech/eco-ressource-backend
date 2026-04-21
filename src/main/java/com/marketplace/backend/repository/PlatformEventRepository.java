package com.marketplace.backend.repository;

import com.marketplace.backend.entity.PlatformEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformEventRepository extends JpaRepository<PlatformEvent, Long> {


}
