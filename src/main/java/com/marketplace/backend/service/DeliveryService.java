package com.marketplace.backend.service;

import com.marketplace.backend.dto.DeliveryRequest;
import com.marketplace.backend.entity.Delivery;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.Transporter;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.DeliveryStatus;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.repository.DeliveryRepository;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.TransporterRepository;
import com.marketplace.backend.security.SecurityUserHelper;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryService {

  private final DeliveryRepository deliveryRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final TransporterRepository transporterRepository;
  private final SecurityUserHelper securityUserHelper;

  @Transactional(readOnly = true)
  public List<Delivery> findAll(Authentication auth) {
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() == Role.ROLE_ADMIN) {
      return deliveryRepository.findAll();
    }
    if (u.getEnterprise() != null) {
      return deliveryRepository.findByEnterpriseId(u.getEnterprise().getId());
    }
    if (u.getTransporter() != null) {
      return deliveryRepository.findByTransporterId(u.getTransporter().getId());
    }
    throw new IllegalArgumentException("Forbidden");
  }

  @Transactional(readOnly = true)
  public Delivery getById(Long id, Authentication auth) {
    Delivery d =
        deliveryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    assertCanAccess(d, securityUserHelper.requireUser(auth));
    return d;
  }

  private void assertCanAccess(Delivery d, User u) {
    if (u.getRole() == Role.ROLE_ADMIN) {
      return;
    }
    if (u.getEnterprise() != null && d.getEnterprise().getId().equals(u.getEnterprise().getId())) {
      return;
    }
    if (u.getTransporter() != null
        && d.getTransporter() != null
        && d.getTransporter().getId().equals(u.getTransporter().getId())) {
      return;
    }
    throw new IllegalArgumentException("Forbidden");
  }

  @Transactional
  public Delivery create(Authentication auth, DeliveryRequest req) {
    User u = securityUserHelper.requireUser(auth);
    Enterprise e =
        enterpriseRepository
            .findById(req.getEnterpriseId())
            .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
    if (u.getRole() != Role.ROLE_ADMIN) {
      if (u.getEnterprise() == null || !u.getEnterprise().getId().equals(e.getId())) {
        throw new IllegalArgumentException("Forbidden");
      }
    }
    Transporter t = null;
    if (req.getTransporterId() != null) {
      t =
          transporterRepository
              .findById(req.getTransporterId())
              .orElseThrow(() -> new IllegalArgumentException("Transporter not found"));
    }
    Delivery d =
        Delivery.builder()
            .productLabel(req.getProductLabel())
            .fromLocation(req.getFromLocation())
            .toLocation(req.getToLocation())
            .clientName(req.getClientName())
            .enterprise(e)
            .transporter(t)
            .status(DeliveryStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)))
            .co2Label(req.getCo2Label())
            .dateLabel(req.getDateLabel())
            .pickupLabel(req.getPickupLabel())
            .deliveryLabel(req.getDeliveryLabel())
            .amount(req.getAmount())
            .earnAmount(req.getEarnAmount())
            .build();
    return deliveryRepository.save(d);
  }

  @Transactional
  public Delivery update(Long id, Authentication auth, DeliveryRequest req) {
    Delivery d =
        deliveryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User u = securityUserHelper.requireUser(auth);
    assertCanAccess(d, u);
    Enterprise e =
        enterpriseRepository
            .findById(req.getEnterpriseId())
            .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
    if (u.getRole() != Role.ROLE_ADMIN) {
      if (u.getEnterprise() == null || !u.getEnterprise().getId().equals(e.getId())) {
        throw new IllegalArgumentException("Forbidden");
      }
    }
    Transporter t = null;
    if (req.getTransporterId() != null) {
      t =
          transporterRepository
              .findById(req.getTransporterId())
              .orElseThrow(() -> new IllegalArgumentException("Transporter not found"));
    }
    d.setEnterprise(e);
    d.setTransporter(t);
    d.setProductLabel(req.getProductLabel());
    d.setFromLocation(req.getFromLocation());
    d.setToLocation(req.getToLocation());
    d.setClientName(req.getClientName());
    d.setStatus(DeliveryStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    d.setCo2Label(req.getCo2Label());
    d.setDateLabel(req.getDateLabel());
    d.setPickupLabel(req.getPickupLabel());
    d.setDeliveryLabel(req.getDeliveryLabel());
    d.setAmount(req.getAmount());
    d.setEarnAmount(req.getEarnAmount());
    return deliveryRepository.save(d);
  }

  @Transactional
  public void delete(Long id, Authentication auth) {
    Delivery d =
        deliveryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    assertCanAccess(d, securityUserHelper.requireUser(auth));
    deliveryRepository.delete(d);
  }
}
