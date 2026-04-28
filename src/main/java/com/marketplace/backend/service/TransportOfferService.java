package com.marketplace.backend.service;

import com.marketplace.backend.dto.TransportOfferRequest;
import com.marketplace.backend.entity.Transporter;
import com.marketplace.backend.entity.TransportOffer;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.TransportOfferStatus;
import com.marketplace.backend.repository.TransporterRepository;
import com.marketplace.backend.repository.TransportOfferRepository;
import com.marketplace.backend.security.SecurityUserHelper;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransportOfferService {

  private final TransportOfferRepository transportOfferRepository;
  private final TransporterRepository transporterRepository;
  private final SecurityUserHelper securityUserHelper;

  private Transporter resolveTransporter(Authentication auth, Long transporterId) {
    User u = securityUserHelper.requireUser(auth);
    if (transporterId != null) {
      Transporter t =
          transporterRepository
              .findById(transporterId)
              .orElseThrow(() -> new IllegalArgumentException("Transporter not found"));
      if (u.getRole() != Role.ROLE_ADMIN) {
        if (u.getTransporter() == null || !u.getTransporter().getId().equals(t.getId())) {
          throw new IllegalArgumentException("Forbidden");
        }
      }
      return t;
    }
    return securityUserHelper.requireTransporter(auth);
  }

  @Transactional(readOnly = true)
  public List<TransportOffer> findAll(Authentication auth) {
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() == Role.ROLE_ADMIN) {
      return transportOfferRepository.findAll();
    }
    if (u.getTransporter() == null) {
      throw new IllegalArgumentException("Transporter profile required");
    }
    return transportOfferRepository.findByTransporterId(u.getTransporter().getId());
  }

  @Transactional(readOnly = true)
  public TransportOffer getById(Long id, Authentication auth) {
    TransportOffer o =
        transportOfferRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() != Role.ROLE_ADMIN
        && (u.getTransporter() == null
            || !o.getTransporter().getId().equals(u.getTransporter().getId()))) {
      throw new IllegalArgumentException("Forbidden");
    }
    return o;
  }

  @Transactional
  public TransportOffer create(Authentication auth, TransportOfferRequest req) {
    Transporter t = resolveTransporter(auth, req.getTransporterId());
    TransportOfferStatus initial = TransportOfferStatus.open;
    if (req.getStatus() != null && !req.getStatus().isBlank()) {
      initial =
          TransportOfferStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT));
    }
    TransportOffer o =
        TransportOffer.builder()
            .transporter(t)
            .fromLocation(req.getFromLocation())
            .toLocation(req.getToLocation())
            .cargoDescription(req.getCargoDescription())
            .weightLabel(req.getWeightLabel())
            .proposedEarn(req.getProposedEarn())
            .status(initial)
            .build();
    return transportOfferRepository.save(o);
  }

  @Transactional
  public TransportOffer update(Long id, Authentication auth, TransportOfferRequest req) {
    TransportOffer o =
        transportOfferRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() != Role.ROLE_ADMIN
        && (u.getTransporter() == null
            || !o.getTransporter().getId().equals(u.getTransporter().getId()))) {
      throw new IllegalArgumentException("Forbidden");
    }
    Transporter t = resolveTransporter(auth, req.getTransporterId() != null ? req.getTransporterId() : o.getTransporter().getId());
    o.setTransporter(t);
    o.setFromLocation(req.getFromLocation());
    o.setToLocation(req.getToLocation());
    o.setCargoDescription(req.getCargoDescription());
    o.setWeightLabel(req.getWeightLabel());
    o.setProposedEarn(req.getProposedEarn());
    if (req.getStatus() != null && !req.getStatus().isBlank()) {
      o.setStatus(TransportOfferStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    }
    return transportOfferRepository.save(o);
  }

  @Transactional
  public void delete(Long id, Authentication auth) {
    TransportOffer o =
        transportOfferRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() != Role.ROLE_ADMIN
        && (u.getTransporter() == null
            || !o.getTransporter().getId().equals(u.getTransporter().getId()))) {
      throw new IllegalArgumentException("Forbidden");
    }
    transportOfferRepository.delete(o);
  }
}
