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

    return toDto(donation);
  }

  @Transactional(readOnly = true)
  public List<DonationResponseDto> getDonationsByAssociation(Long associationId) {
    return donationRepository.findByAssociationId(associationId)
        .stream()
        .map(this::toDto)
        .collect(Collectors.toList());
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
