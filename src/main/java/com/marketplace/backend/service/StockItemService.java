package com.marketplace.backend.service;

import com.marketplace.backend.dto.StockItemRequest;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.StockItemStatus;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.StockItemRepository;
import com.marketplace.backend.security.SecurityUserHelper;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockItemService {

  private final StockItemRepository stockItemRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final SecurityUserHelper securityUserHelper;

  private void assertCanAccessEnterprise(User u, Long enterpriseId) {
    if (u.getRole() == Role.ROLE_ADMIN) {
      return;
    }
    if (u.getEnterprise() == null || !u.getEnterprise().getId().equals(enterpriseId)) {
      throw new IllegalArgumentException("Forbidden");
    }
  }

  @Transactional(readOnly = true)
  public List<StockItem> findAll(Authentication auth) {
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() == Role.ROLE_ADMIN) {
      return stockItemRepository.findAll();
    }
    if (u.getEnterprise() == null) {
      throw new IllegalArgumentException("Enterprise profile required");
    }
    return stockItemRepository.findByEnterpriseId(u.getEnterprise().getId());
  }

  @Transactional(readOnly = true)
  public StockItem getById(Long id, Authentication auth) {
    StockItem s =
        stockItemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    assertCanAccessEnterprise(securityUserHelper.requireUser(auth), s.getEnterprise().getId());
    return s;
  }

  @Transactional
  public StockItem create(Authentication auth, StockItemRequest req) {
    User u = securityUserHelper.requireUser(auth);
    assertCanAccessEnterprise(u, req.getEnterpriseId());
    Enterprise e =
        enterpriseRepository
            .findById(req.getEnterpriseId())
            .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
    StockItem s =
        StockItem.builder()
            .enterprise(e)
            .name(req.getName())
            .category(req.getCategory())
            .quantity(req.getQuantity())
            .unit(req.getUnit())
            .conditionLabel(req.getConditionLabel())
            .status(StockItemStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)))
            .aiInsight(req.getAiInsight())
            .build();
    return stockItemRepository.save(s);
  }

  @Transactional
  public StockItem update(Long id, Authentication auth, StockItemRequest req) {
    StockItem s =
        stockItemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User u = securityUserHelper.requireUser(auth);
    assertCanAccessEnterprise(u, s.getEnterprise().getId());
    assertCanAccessEnterprise(u, req.getEnterpriseId());
    Enterprise e =
        enterpriseRepository
            .findById(req.getEnterpriseId())
            .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
    s.setEnterprise(e);
    s.setName(req.getName());
    s.setCategory(req.getCategory());
    s.setQuantity(req.getQuantity());
    s.setUnit(req.getUnit());
    s.setConditionLabel(req.getConditionLabel());
    s.setStatus(StockItemStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    s.setAiInsight(req.getAiInsight());
    return stockItemRepository.save(s);
  }

  @Transactional
  public void delete(Long id, Authentication auth) {
    StockItem s =
        stockItemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    assertCanAccessEnterprise(securityUserHelper.requireUser(auth), s.getEnterprise().getId());
    stockItemRepository.delete(s);
  }
}
