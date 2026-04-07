package com.marketplace.backend.service;

import com.marketplace.backend.dto.SolidarityAssociationRequest;
import com.marketplace.backend.entity.SolidarityAssociation;
import com.marketplace.backend.repository.SolidarityAssociationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SolidarityAssociationService {

  private final SolidarityAssociationRepository solidarityAssociationRepository;

  @Transactional(readOnly = true)
  public List<SolidarityAssociation> findAll() {
    return solidarityAssociationRepository.findAll();
  }

  @Transactional(readOnly = true)
  public SolidarityAssociation getById(Long id) {
    return solidarityAssociationRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Not found"));
  }

  @Transactional
  public SolidarityAssociation create(SolidarityAssociationRequest req) {
    SolidarityAssociation s =
        SolidarityAssociation.builder()
            .name(req.getName())
            .mission(req.getMission())
            .members(req.getMembers())
            .donations(req.getDonations())
            .statusLabel(req.getStatusLabel())
            .aiInsight(req.getAiInsight())
            .build();
    return solidarityAssociationRepository.save(s);
  }

  @Transactional
  public SolidarityAssociation update(Long id, SolidarityAssociationRequest req) {
    SolidarityAssociation s =
        solidarityAssociationRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));
    s.setName(req.getName());
    s.setMission(req.getMission());
    s.setMembers(req.getMembers());
    s.setDonations(req.getDonations());
    s.setStatusLabel(req.getStatusLabel());
    s.setAiInsight(req.getAiInsight());
    return solidarityAssociationRepository.save(s);
  }

  @Transactional
  public void delete(Long id) {
    SolidarityAssociation s =
        solidarityAssociationRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));
    solidarityAssociationRepository.delete(s);
  }
}
