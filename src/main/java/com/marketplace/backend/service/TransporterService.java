package com.marketplace.backend.service;

import com.marketplace.backend.dto.TransporterRequest;
import com.marketplace.backend.entity.Transporter;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.repository.TransporterRepository;
import com.marketplace.backend.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransporterService {

  private final TransporterRepository transporterRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<Transporter> findAll() {
    return transporterRepository.findAll();
  }

  @Transactional(readOnly = true)
  public Transporter getById(Long id) {
    return transporterRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
  }

  @Transactional
  public Transporter create(TransporterRequest req) {
    User user =
        userRepository.findById(req.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (user.getRole() != Role.ROLE_TRANSPORTER) {
      throw new IllegalArgumentException("User must be transporter role");
    }
    if (user.getTransporter() != null) {
      throw new IllegalArgumentException("User already has transporter profile");
    }
    Transporter t =
        Transporter.builder()
            .user(user)
            .companyName(req.getCompanyName())
            .sector(req.getSector())
            .taxId(req.getTaxId())
            .listingsCount(req.getListingsCount() != null ? req.getListingsCount() : 0)
            .ordersCount(req.getOrdersCount() != null ? req.getOrdersCount() : 0)
            .revenue(req.getRevenue() != null ? req.getRevenue() : "0")
            .build();
    user.setTransporter(t);
    return transporterRepository.save(t);
  }

  @Transactional
  public Transporter update(Long id, TransporterRequest req) {
    Transporter t =
        transporterRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User user =
        userRepository.findById(req.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (!t.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Cannot reassign user on transporter");
    }
    t.setCompanyName(req.getCompanyName());
    t.setSector(req.getSector());
    t.setTaxId(req.getTaxId());
    if (req.getListingsCount() != null) {
      t.setListingsCount(req.getListingsCount());
    }
    if (req.getOrdersCount() != null) {
      t.setOrdersCount(req.getOrdersCount());
    }
    if (req.getRevenue() != null) {
      t.setRevenue(req.getRevenue());
    }
    return transporterRepository.save(t);
  }

  @Transactional
  public void delete(Long id) {
    Transporter t =
        transporterRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User u = t.getUser();
    u.setTransporter(null);
    userRepository.save(u);
  }
}
