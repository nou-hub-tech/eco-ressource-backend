package com.marketplace.backend.service;

import com.marketplace.backend.dto.EnterpriseRequest;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnterpriseService {

  private final EnterpriseRepository enterpriseRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<Enterprise> findAll() {
    return enterpriseRepository.findAll();
  }

  @Transactional(readOnly = true)
  public Enterprise getById(Long id) {
    return enterpriseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
  }

  @Transactional
  public Enterprise create(EnterpriseRequest req) {
    User user =
        userRepository.findById(req.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (user.getRole() != Role.ROLE_ENTERPRISE) {
      throw new IllegalArgumentException("User must be enterprise role");
    }
    if (user.getEnterprise() != null) {
      throw new IllegalArgumentException("User already has enterprise profile");
    }
    Enterprise e =
        Enterprise.builder()
            .user(user)
            .companyName(req.getCompanyName())
            .sector(req.getSector())
            .taxId(req.getTaxId())
            .listingsCount(req.getListingsCount() != null ? req.getListingsCount() : 0)
            .ordersCount(req.getOrdersCount() != null ? req.getOrdersCount() : 0)
            .revenue(req.getRevenue() != null ? req.getRevenue() : "0")
            .build();
    user.setEnterprise(e);
    return enterpriseRepository.save(e);
  }

  @Transactional
  public Enterprise update(Long id, EnterpriseRequest req) {
    Enterprise e =
        enterpriseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User user =
        userRepository.findById(req.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (!e.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Cannot reassign user on enterprise");
    }
    e.setCompanyName(req.getCompanyName());
    e.setSector(req.getSector());
    e.setTaxId(req.getTaxId());
    if (req.getListingsCount() != null) {
      e.setListingsCount(req.getListingsCount());
    }
    if (req.getOrdersCount() != null) {
      e.setOrdersCount(req.getOrdersCount());
    }
    if (req.getRevenue() != null) {
      e.setRevenue(req.getRevenue());
    }
    return enterpriseRepository.save(e);
  }

  @Transactional
  public void delete(Long id) {
    Enterprise e =
        enterpriseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User u = e.getUser();
    u.setEnterprise(null);
    userRepository.save(u);
  }
}
