package com.marketplace.backend.service;

import com.marketplace.backend.dto.DonationRequest;
import com.marketplace.backend.dto.DonationResponseDto;
import com.marketplace.backend.entity.Donation;
import com.marketplace.backend.entity.SolidarityAssociation;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.repository.DonationRepository;
import com.marketplace.backend.repository.SolidarityAssociationRepository;
import com.marketplace.backend.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DonationService {

  private final DonationRepository donationRepository;
  private final SolidarityAssociationRepository associationRepository;
  private final UserRepository userRepository;
  private final AiService aiService;

  @Transactional
  public DonationResponseDto createDonation(DonationRequest request) {
    SolidarityAssociation association =
        associationRepository
            .findById(request.getAssociationId())
            .orElseThrow(() -> new IllegalArgumentException("Association not found"));

    User user = null;
    if (request.getUserId() != null) {
      user = userRepository
          .findById(request.getUserId())
          .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    Donation donation = new Donation();
    donation.setAmount(request.getAmount());
    donation.setMessage(request.getMessage());
    donation.setAssociation(association);
    donation.setUser(user);

    donation = donationRepository.save(donation);

    // Update the association's total donations amount
    Double currentTotal = association.getDonations();
    if (currentTotal == null) currentTotal = 0.0;
    association.setDonations(currentTotal + request.getAmount());
    associationRepository.save(association);
    
    // Refresh AI Insight asynchronously
    aiService.updateAssociationInsightAsync(association.getId());

    return toDto(donation);

  }


  @Transactional(readOnly = true)
  public List<DonationResponseDto> getDonationsByAssociation(Long associationId) {
    return donationRepository.findByAssociationId(associationId)
        .stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public void delete(Long id, User currentUser) {
    System.out.println("[DonationService] Attempting to delete donation ID: " + id);
    System.out.println("[DonationService] Current User: " + currentUser.getEmail() + " (Role: " + currentUser.getRole() + ")");

    Donation d = donationRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Donation not found with ID: " + id));

    // Admin can delete anything. Enterprise users can only delete their own donations.
    boolean isAdmin = currentUser.getRole() == com.marketplace.backend.entity.enums.Role.ROLE_ADMIN;
    boolean isOwner = d.getUser() != null && d.getUser().getId().equals(currentUser.getId());

    System.out.println("[DonationService] Permission check: isAdmin=" + isAdmin + ", isOwner=" + isOwner);

    if (!isAdmin && !isOwner) {
      System.out.println("[DonationService] Access Denied for user " + currentUser.getEmail());
      throw new org.springframework.security.access.AccessDeniedException(
          "You do not have permission to delete this donation. Only the donor or an administrator can perform this action."
      );
    }

    // Adjust association's total donations
    SolidarityAssociation assoc = d.getAssociation();
    if (assoc != null) {
      Double newTotal = (assoc.getDonations() != null ? assoc.getDonations() : 0.0) - d.getAmount();
      assoc.setDonations(Math.max(0, newTotal));
      associationRepository.save(assoc);
      
      // Refresh AI Insight
      aiService.updateAssociationInsightAsync(assoc.getId());
    }

    donationRepository.delete(d);
  }

  private DonationResponseDto toDto(Donation d) {
    return DonationResponseDto.builder()
        .id(d.getId())
        .amount(d.getAmount())
        .message(d.getMessage())
        .associationId(d.getAssociation() != null ? d.getAssociation().getId() : null)
        .userId(d.getUser() != null ? d.getUser().getId() : null)
        .userName(d.getUser() != null ? d.getUser().getFullName() : null)
        .createdAt(d.getCreatedAt())
        .build();
  }
}
