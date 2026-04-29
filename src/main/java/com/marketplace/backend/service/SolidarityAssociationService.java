package com.marketplace.backend.service;

import com.marketplace.backend.dto.SolidarityAssociationRequest;
import com.marketplace.backend.entity.SolidarityAssociation;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.repository.SolidarityAssociationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SolidarityAssociationService {

  private final SolidarityAssociationRepository solidarityAssociationRepository;
  private final AiService aiService;

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
  public SolidarityAssociation create(SolidarityAssociationRequest req, Long userId) {
    SolidarityAssociation s =
        SolidarityAssociation.builder()
            .name(req.getName())
            .mission(req.getMission())
            .members(req.getMembers())
            .donations(req.getDonations())
            .statusLabel(req.getStatusLabel())
            .aiInsight(req.getAiInsight())
            .goalAmount(req.getGoalAmount())
            .userId(userId)
            .build();
    
    SolidarityAssociation saved = solidarityAssociationRepository.save(s);
    
    // Generate AI Insight asynchronously
    aiService.updateAssociationInsightAsync(saved.getId());
    
    return saved;
  }

  @Transactional
  public SolidarityAssociation update(Long id, SolidarityAssociationRequest req, User currentUser) {
    SolidarityAssociation s =
        solidarityAssociationRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solidarity Association not found with ID: " + id));

    boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;
    boolean isOwner = s.getUserId() != null && s.getUserId().equals(currentUser.getId());

    if (!isAdmin && !isOwner) {
      throw new org.springframework.security.access.AccessDeniedException(
          "You do not have permission to update this association."
      );
    }

    s.setName(req.getName());
    s.setMission(req.getMission());
    s.setMembers(req.getMembers());
    // s.setDonations(req.getDonations()); // User requested: "cant edit amount of money"
    s.setStatusLabel(req.getStatusLabel());
    s.setAiInsight(req.getAiInsight());
    s.setGoalAmount(req.getGoalAmount());
    
    SolidarityAssociation saved = solidarityAssociationRepository.save(s);
    
    // Refresh AI Insight
    aiService.updateAssociationInsightAsync(saved.getId());
    
    return saved;
  }

  @Transactional
  public void delete(Long id, User currentUser) {
    SolidarityAssociation s =
        solidarityAssociationRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solidarity Association not found with ID: " + id));
            
    boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;
    boolean isOwner = s.getUserId() != null && s.getUserId().equals(currentUser.getId());

    if (!isAdmin && !isOwner) {
      throw new org.springframework.security.access.AccessDeniedException(
          "You do not have permission to delete this association. Only the creator or an administrator can perform this action."
      );
    }
    
    solidarityAssociationRepository.delete(s);
  }

  @Transactional
  public void deleteAllAssociations() {
    solidarityAssociationRepository.deleteAll();
  }
}
