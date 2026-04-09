package com.marketplace.backend.repository;

import com.marketplace.backend.entity.TransportOffer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportOfferRepository extends JpaRepository<TransportOffer, Long> {

  List<TransportOffer> findByTransporterId(Long transporterId);
}
