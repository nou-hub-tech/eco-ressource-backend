package com.marketplace.backend.service;

import com.marketplace.backend.dto.ReservationRequest;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.Reservation;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.ReservationStatus;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.ReservationRepository;
import com.marketplace.backend.security.SecurityUserHelper;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {

  private final ReservationRepository reservationRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final SecurityUserHelper securityUserHelper;

  private Enterprise resolveEnterprise(Authentication auth, Long enterpriseId) {
    User u = securityUserHelper.requireUser(auth);
    if (enterpriseId != null) {
      Enterprise e =
          enterpriseRepository
              .findById(enterpriseId)
              .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
      if (u.getRole() != Role.ROLE_ADMIN) {
        if (u.getEnterprise() == null || !u.getEnterprise().getId().equals(e.getId())) {
          throw new IllegalArgumentException("Forbidden");
        }
      }
      return e;
    }
    if (u.getRole() == Role.ROLE_ADMIN) {
      throw new IllegalArgumentException("enterpriseId is required");
    }
    return securityUserHelper.requireEnterpriseStrict(auth);
  }

  @Transactional(readOnly = true)
  public List<Reservation> findAll(Authentication auth) {
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() == Role.ROLE_ADMIN) {
      return reservationRepository.findAll();
    }
    if (u.getEnterprise() == null) {
      throw new IllegalArgumentException("Enterprise profile required");
    }
    return reservationRepository.findByEnterpriseId(u.getEnterprise().getId());
  }

  @Transactional(readOnly = true)
  public Reservation getById(Long id, Authentication auth) {
    Reservation r =
        reservationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    assertCanRead(securityUserHelper.requireUser(auth), r);
    return r;
  }

  private void assertCanRead(User u, Reservation r) {
    if (u.getRole() == Role.ROLE_ADMIN) {
      return;
    }
    if (r.getEnterprise() == null || u.getEnterprise() == null) {
      throw new IllegalArgumentException("Forbidden");
    }
    if (!r.getEnterprise().getId().equals(u.getEnterprise().getId())) {
      throw new IllegalArgumentException("Forbidden");
    }
  }

  @Transactional
  public Reservation create(Authentication auth, ReservationRequest req) {
    Enterprise e = resolveEnterprise(auth, req.getEnterpriseId());
    Reservation r =
        Reservation.builder()
            .typeLabel(req.getTypeLabel())
            .item(req.getItem())
            .companyName(req.getCompanyName())
            .fromDate(req.getFromDate())
            .toDate(req.getToDate())
            .price(req.getPrice())
            .status(ReservationStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)))
            .enterprise(e)
            .build();
    return reservationRepository.save(r);
  }

  @Transactional
  public Reservation update(Long id, Authentication auth, ReservationRequest req) {
    Reservation r =
        reservationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User u = securityUserHelper.requireUser(auth);
    assertCanRead(u, r);
    if (req.getEnterpriseId() != null) {
      Enterprise e =
          enterpriseRepository
              .findById(req.getEnterpriseId())
              .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
      if (u.getRole() != Role.ROLE_ADMIN) {
        if (u.getEnterprise() == null || !u.getEnterprise().getId().equals(e.getId())) {
          throw new IllegalArgumentException("Forbidden");
        }
      }
      r.setEnterprise(e);
    }
    r.setTypeLabel(req.getTypeLabel());
    r.setItem(req.getItem());
    r.setCompanyName(req.getCompanyName());
    r.setFromDate(req.getFromDate());
    r.setToDate(req.getToDate());
    r.setPrice(req.getPrice());
    r.setStatus(ReservationStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    return reservationRepository.save(r);
  }

  @Transactional
  public void delete(Long id, Authentication auth) {
    Reservation r =
        reservationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    assertCanRead(securityUserHelper.requireUser(auth), r);
    reservationRepository.delete(r);
  }
}
