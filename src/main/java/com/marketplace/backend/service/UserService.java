package com.marketplace.backend.service;

import com.marketplace.backend.dto.AdminUserDto;
import com.marketplace.backend.dto.EventDto;
import com.marketplace.backend.dto.ReservationDto;
import com.marketplace.backend.dto.SolidarityDto;
import com.marketplace.backend.dto.UserStatusRequest;
import com.marketplace.backend.dto.WalletTransactionDto;
import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.entity.Reservation;
import com.marketplace.backend.entity.SolidarityAssociation;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.WalletTransaction;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.UserAccountStatus;
import com.marketplace.backend.repository.PlatformEventRepository;
import com.marketplace.backend.repository.ReservationRepository;
import com.marketplace.backend.repository.SolidarityAssociationRepository;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.repository.WalletTransactionRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PlatformEventRepository platformEventRepository;
  private final ReservationRepository reservationRepository;
  private final SolidarityAssociationRepository solidarityAssociationRepository;
  private final WalletTransactionRepository walletTransactionRepository;

  @Transactional(readOnly = true)
  public List<AdminUserDto> listNonAdminUsers() {
    return userRepository.findAllWithProfiles().stream()
        .filter(u -> u.getRole() != Role.ROLE_ADMIN)
        .map(userMapper::toAdminUserDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public AdminUserDto updateStatus(Long userId, UserStatusRequest req) {
    User u = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Not found"));
    if (u.getRole() == Role.ROLE_ADMIN) {
      throw new IllegalArgumentException("Cannot modify admin");
    }
    UserAccountStatus st =
        switch (req.getStatus()) {
          case "active" -> UserAccountStatus.active;
          case "pending" -> UserAccountStatus.pending;
          case "suspended" -> UserAccountStatus.suspended;
          default -> throw new IllegalArgumentException("Invalid status");
        };
    u.setAccountStatus(st);
    if ("active".equals(req.getStatus())) {
      u.setVerified(true);
    }
    userRepository.save(u);
    User fresh =
        userRepository
            .findByIdWithProfiles(userId)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));
    return userMapper.toAdminUserDto(fresh);
  }

  @Transactional
  public void deleteUser(Long userId) {
    User u = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Not found"));
    if (u.getRole() == Role.ROLE_ADMIN) {
      throw new IllegalArgumentException("Cannot delete admin");
    }
    userRepository.delete(u);
  }

  @Transactional(readOnly = true)
  public List<EventDto> allEvents() {
    return platformEventRepository.findAll().stream()
        .map(this::toEventDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<ReservationDto> allReservations() {
    return reservationRepository.findAll().stream()
        .map(this::toReservationDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<SolidarityDto> allSolidarity() {
    return solidarityAssociationRepository.findAll().stream()
        .map(this::toSolidarityDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<WalletTransactionDto> allWalletTransactions() {
    return walletTransactionRepository.findAll().stream()
        .map(this::toWalletDto)
        .collect(Collectors.toList());
  }

  private EventDto toEventDto(PlatformEvent e) {
    return EventDto.builder()
        .title(e.getTitle())
        .date(e.getEventDate().toString())
        .location(e.getLocation())
        .participants(e.getParticipants())
        .status(e.getStatus().name())
        .type(e.getTypeLabel())
        .build();
  }

  private ReservationDto toReservationDto(Reservation r) {
    return ReservationDto.builder()
        .id("RES-" + String.format("%03d", r.getId()))
        .type(r.getTypeLabel())
        .item(r.getItem())
        .company(r.getCompanyName())
        .from(r.getFromDate().toString())
        .to(r.getToDate().toString())
        .price(r.getPrice())
        .status(r.getStatus().name())
        .build();
  }

  private SolidarityDto toSolidarityDto(SolidarityAssociation s) {
    return SolidarityDto.builder()
        .id(s.getId())
        .name(s.getName())
        .mission(s.getMission())
        .members(s.getMembers())
        .donations(s.getDonations())
        .status(s.getStatusLabel())
        .ai(s.getAiInsight())
        .build();
  }

  private WalletTransactionDto toWalletDto(WalletTransaction t) {
    return WalletTransactionDto.builder()
        .id("TXN-" + t.getId())
        .label(t.getLabel())
        .type(t.getTypeLabel())
        .amount(t.getAmount())
        .positive(t.getPositiveFlag())
        .status(t.getStatus().name())
        .date(t.getValueDate() != null ? t.getValueDate().toString() : "")
        .from(t.getFromParty())
        .to(t.getToParty())
        .build();
  }
}
