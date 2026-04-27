package com.marketplace.backend.service;

import com.marketplace.backend.dto.GroupPurchaseResponse;
import com.marketplace.backend.dto.JoinGroupRequest;
import com.marketplace.backend.entity.GroupParticipant;
import com.marketplace.backend.entity.GroupPurchase;
import com.marketplace.backend.entity.enums.GroupPurchaseStatus;
import com.marketplace.backend.entity.enums.ResourceListingStatus;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.GroupParticipantRepository;
import com.marketplace.backend.repository.GroupPurchaseRepository;
import com.marketplace.backend.repository.TransporterRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupPurchaseService {

  private final GroupPurchaseRepository groupPurchaseRepository;
  private final GroupParticipantRepository participantRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final TransporterRepository transporterRepository;
  private final RealtimeNotificationService realtimeNotificationService;

  @Transactional(readOnly = true)
  public GroupPurchaseResponse getById(Long groupId) {
    GroupPurchase group =
        groupPurchaseRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));
    GroupPurchaseResponse response = toResponse(group);
    realtimeNotificationService.groupChanged(group.getListing().getId(), group.getId(), response);
    notifyListingOwner(group, "GROUP_JOINED", "Une entreprise a rejoint votre achat groupe", response);
    return response;
  }

  @Transactional
  public GroupPurchaseResponse join(Long groupId, JoinGroupRequest req) {
    GroupPurchase group =
        groupPurchaseRepository
            .findByIdForUpdate(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (group.getStatus() != GroupPurchaseStatus.OPEN) {
      throw new IllegalArgumentException("Group is not open for participation");
    }

    if (group.getListing().getStatus() != ResourceListingStatus.ACTIVE) {
      throw new IllegalArgumentException("Listing is no longer active");
    }

    if (group.getDeadline().isBefore(LocalDateTime.now())) {
      group.setStatus(GroupPurchaseStatus.FAILED);
      groupPurchaseRepository.save(group);
      throw new IllegalArgumentException("Group deadline has passed");
    }

    if (group.getListing().getCompanyId().equals(req.getCompanyId())) {
      throw new IllegalArgumentException("The seller cannot join their own group");
    }

    if (participantRepository.existsByGroupIdAndCompanyId(groupId, req.getCompanyId())) {
      throw new IllegalArgumentException(
          "This company has already joined this group");
    }

    int remaining = group.getTargetQuantity() - group.getCurrentQuantity();
    if (req.getQuantity() > remaining) {
      throw new IllegalArgumentException(
          "Requested quantity exceeds remaining capacity (" + remaining + ")");
    }

    GroupParticipant participant =
        GroupParticipant.builder()
            .group(group)
            .companyId(req.getCompanyId())
            .quantity(req.getQuantity())
            .build();
    participantRepository.save(participant);

    group.setCurrentQuantity(group.getCurrentQuantity() + req.getQuantity());

    if (group.getCurrentQuantity() >= group.getTargetQuantity()) {
      group.setStatus(GroupPurchaseStatus.FULL);
    }

    groupPurchaseRepository.save(group);

    GroupPurchaseResponse response = toResponse(group);
    realtimeNotificationService.groupChanged(group.getListing().getId(), group.getId(), response);
    notifyListingOwner(group, "GROUP_LEFT", "Une entreprise a quitte votre achat groupe", response);
    return response;
  }

  @Transactional
  public GroupPurchaseResponse leave(Long groupId, Long companyId) {
    GroupPurchase group =
        groupPurchaseRepository
            .findByIdForUpdate(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (group.getStatus() != GroupPurchaseStatus.OPEN
        && group.getStatus() != GroupPurchaseStatus.FULL) {
      throw new IllegalArgumentException("Cannot leave a group that is " + group.getStatus());
    }

    GroupParticipant participant =
        participantRepository
            .findByGroupIdAndCompanyId(groupId, companyId)
            .orElseThrow(
                () -> new IllegalArgumentException("Participation not found"));

    group.setCurrentQuantity(group.getCurrentQuantity() - participant.getQuantity());

    if (group.getStatus() == GroupPurchaseStatus.FULL
        && group.getCurrentQuantity() < group.getTargetQuantity()) {
      group.setStatus(GroupPurchaseStatus.OPEN);
    }

    participantRepository.delete(participant);
    groupPurchaseRepository.save(group);

    return toResponse(group);
  }

  @Transactional(readOnly = true)
  public List<GroupPurchaseResponse.ParticipantInfo> getParticipants(Long groupId) {
    groupPurchaseRepository
        .findById(groupId)
        .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    return participantRepository.findByGroupId(groupId).stream()
        .map(
            p ->
                GroupPurchaseResponse.ParticipantInfo.builder()
                    .id(p.getId())
                    .companyId(p.getCompanyId())
                    .companyName(resolveCompanyName(p.getCompanyId()))
                    .quantity(p.getQuantity())
                    .build())
        .collect(Collectors.toList());
  }

  private GroupPurchaseResponse toResponse(GroupPurchase group) {
    List<GroupPurchaseResponse.ParticipantInfo> participants =
        participantRepository.findByGroupId(group.getId()).stream()
            .map(
                p ->
                    GroupPurchaseResponse.ParticipantInfo.builder()
                        .id(p.getId())
                        .companyId(p.getCompanyId())
                        .companyName(resolveCompanyName(p.getCompanyId()))
                        .quantity(p.getQuantity())
                        .build())
            .collect(Collectors.toList());

    return GroupPurchaseResponse.builder()
        .id(group.getId())
        .listingId(group.getListing().getId())
        .targetQuantity(group.getTargetQuantity())
        .currentQuantity(group.getCurrentQuantity())
        .remainingQuantity(group.getTargetQuantity() - group.getCurrentQuantity())
        .deadline(group.getDeadline())
        .status(group.getStatus().name())
        .participants(participants)
        .build();
  }

  private String resolveCompanyName(Long companyId) {
    if (companyId == null) {
      return null;
    }
    return enterpriseRepository
        .findById(companyId)
        .map(e -> e.getCompanyName())
        .orElseGet(
            () ->
                transporterRepository
                    .findById(companyId)
                    .map(t -> t.getCompanyName())
                .orElse(null));
  }

  private void notifyListingOwner(GroupPurchase group, String type, String message, Object payload) {
    Long companyId = group.getListing().getCompanyId();
    Long ownerUserId =
        enterpriseRepository
            .findById(companyId)
            .map(e -> e.getUser().getId())
            .orElseGet(
                () ->
                    transporterRepository
                        .findById(companyId)
                        .map(t -> t.getUser().getId())
                        .orElse(null));
    if (ownerUserId != null) {
      realtimeNotificationService.notifyUser(ownerUserId, type, message, payload);
    }
  }
}
