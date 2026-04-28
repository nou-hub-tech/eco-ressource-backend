package com.marketplace.backend.service;

import com.marketplace.backend.dto.ExchangeRequestRequest;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.ExchangeRequest;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.ExchangeRequestStatus;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.ExchangeRequestRepository;
import com.marketplace.backend.security.SecurityUserHelper;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExchangeRequestService {

  private final ExchangeRequestRepository exchangeRequestRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final SecurityUserHelper securityUserHelper;

  @Transactional(readOnly = true)
  public List<ExchangeRequest> findAll(Authentication auth) {
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() == Role.ROLE_ADMIN) {
      return exchangeRequestRepository.findAll();
    }
    if (u.getEnterprise() == null) {
      throw new IllegalArgumentException("Enterprise profile required");
    }
    return exchangeRequestRepository.findByRecipientEnterpriseId(u.getEnterprise().getId());
  }

  @Transactional(readOnly = true)
  public ExchangeRequest getById(Long id, Authentication auth) {
    ExchangeRequest r =
        exchangeRequestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() != Role.ROLE_ADMIN) {
      assertCanAccess(u, r);
    }
    return r;
  }

  private void assertCanAccess(User u, ExchangeRequest r) {
    if (u.getRole() == Role.ROLE_ADMIN) {
      return;
    }
    Enterprise e = u.getEnterprise();
    if (e == null || !r.getRecipientEnterprise().getId().equals(e.getId())) {
      throw new IllegalArgumentException("Forbidden");
    }
  }

  @Transactional
  public ExchangeRequest create(Authentication auth, ExchangeRequestRequest req) {
    Enterprise recipient =
        enterpriseRepository
            .findById(req.getRecipientEnterpriseId())
            .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() != Role.ROLE_ADMIN && u.getEnterprise() != null) {
      if (u.getEnterprise().getId().equals(recipient.getId())) {
        throw new IllegalArgumentException("Cannot request self");
      }
    }
    ExchangeRequest r =
        ExchangeRequest.builder()
            .recipientEnterprise(recipient)
            .fromCompanyName(req.getFromCompanyName())
            .fromAvatar(req.getFromAvatar())
            .item(req.getItem())
            .typeLabel(req.getTypeLabel())
            .fromDate(req.getFromDate())
            .toDate(req.getToDate())
            .durationLabel(req.getDurationLabel())
            .price(req.getPrice())
            .message(req.getMessage())
            .status(
                ExchangeRequestStatus.valueOf(
                    req.getStatus().toLowerCase(Locale.ROOT)))
            .receivedLabel(req.getReceivedLabel())
            .urgent(req.isUrgent())
            .build();
    return exchangeRequestRepository.save(r);
  }

  @Transactional
  public ExchangeRequest update(Long id, Authentication auth, ExchangeRequestRequest req) {
    ExchangeRequest r =
        exchangeRequestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() != Role.ROLE_ADMIN) {
      assertCanAccess(u, r);
    }
    Enterprise recipient =
        enterpriseRepository
            .findById(req.getRecipientEnterpriseId())
            .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
    r.setRecipientEnterprise(recipient);
    r.setFromCompanyName(req.getFromCompanyName());
    r.setFromAvatar(req.getFromAvatar());
    r.setItem(req.getItem());
    r.setTypeLabel(req.getTypeLabel());
    r.setFromDate(req.getFromDate());
    r.setToDate(req.getToDate());
    r.setDurationLabel(req.getDurationLabel());
    r.setPrice(req.getPrice());
    r.setMessage(req.getMessage());
    r.setStatus(ExchangeRequestStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    r.setReceivedLabel(req.getReceivedLabel());
    r.setUrgent(req.isUrgent());
    return exchangeRequestRepository.save(r);
  }

  @Transactional
  public ExchangeRequest updateStatus(Long id, String status, Authentication auth) {
    User u = securityUserHelper.requireUser(auth);
    ExchangeRequest r =
        exchangeRequestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    if (u.getRole() != Role.ROLE_ADMIN) {
      Enterprise e = u.getEnterprise();
      if (e == null || !r.getRecipientEnterprise().getId().equals(e.getId())) {
        throw new IllegalArgumentException("Forbidden");
      }
    }
    r.setStatus(ExchangeRequestStatus.valueOf(status.toLowerCase(Locale.ROOT)));
    return exchangeRequestRepository.save(r);
  }

  @Transactional
  public void delete(Long id, Authentication auth) {
    ExchangeRequest r =
        exchangeRequestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() != Role.ROLE_ADMIN) {
      assertCanAccess(u, r);
    }
    exchangeRequestRepository.delete(r);
  }
}
