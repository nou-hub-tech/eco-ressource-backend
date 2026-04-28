package com.marketplace.backend.service;

import com.marketplace.backend.dto.AdminUserDto;
import com.marketplace.backend.dto.EventDto;
import com.marketplace.backend.dto.ReservationDto;
import com.marketplace.backend.dto.SolidarityDto;
import com.marketplace.backend.dto.UserCreateRequest;
import com.marketplace.backend.dto.UserStatusRequest;
import com.marketplace.backend.dto.UserUpdateRequest;
import com.marketplace.backend.dto.WalletTransactionDto;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.entity.Reservation;
import com.marketplace.backend.entity.SolidarityAssociation;
import com.marketplace.backend.entity.Transporter;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  private final PlatformEventRepository platformEventRepository;
  private final ReservationRepository reservationRepository;
  private final SolidarityAssociationRepository solidarityAssociationRepository;
  private final WalletTransactionRepository walletTransactionRepository;

  @Transactional(readOnly = true)
  public List<AdminUserDto> findAllForAdmin() {
    return userRepository.findAllWithProfiles()
            .stream()
            .map(userMapper::toAdminUserDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<AdminUserDto> listNonAdminUsers() {
    return userRepository.findAllWithProfiles()
            .stream()
            .filter(u -> u.getRole() != Role.ROLE_ADMIN)
            .map(userMapper::toAdminUserDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public long countNonAdminUsers() {
    return userRepository.findAll()
            .stream()
            .filter(u -> u.getRole() != Role.ROLE_ADMIN)
            .count();
  }

  @Transactional(readOnly = true)
  public AdminUserDto getForAdmin(Long id) {
    User user = userRepository.findByIdWithProfiles(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    return userMapper.toAdminUserDto(user);
  }

  @Transactional
  public AdminUserDto create(UserCreateRequest req) {

    if (userRepository.existsByEmail(req.getEmail())) {
      throw new IllegalArgumentException("Email already registered");
    }

    Role role = "transporter".equalsIgnoreCase(req.getRole())
            ? Role.ROLE_TRANSPORTER
            : Role.ROLE_ENTERPRISE;

    User user = User.builder()
            .email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword()))
            .fullName(req.getName())
            .role(role)
            .enabled(true)
            .accountStatus(UserAccountStatus.active)
            .phone(req.getPhone())
            .verified(false)
            .build();

    if (role == Role.ROLE_ENTERPRISE) {
      Enterprise enterprise = Enterprise.builder()
              .user(user)
              .companyName(req.getCompanyName())
              .sector(req.getSector())
              .taxId(req.getTaxId())
              .listingsCount(0)
              .ordersCount(0)
              .revenue("0")
              .build();

      user.setEnterprise(enterprise);

    } else {
      Transporter transporter = Transporter.builder()
              .user(user)
              .companyName(req.getCompanyName())
              .sector(req.getSector())
              .taxId(req.getTaxId())
              .listingsCount(0)
              .ordersCount(0)
              .revenue("0")
              .build();

      user.setTransporter(transporter);
    }

    userRepository.save(user);

    User fresh = userRepository.findByIdWithProfiles(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    return userMapper.toAdminUserDto(fresh);
  }

  @Transactional
  public AdminUserDto update(Long userId, UserUpdateRequest req) {

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (user.getRole() == Role.ROLE_ADMIN && req.getAccountStatus() != null) {
      throw new IllegalArgumentException("Cannot change admin account");
    }

    if (req.getFullName() != null) {
      user.setFullName(req.getFullName());
    }

    if (req.getPhone() != null) {
      user.setPhone(req.getPhone());
    }

    if (req.getCity() != null) {
      user.setCity(req.getCity());
    }

    if (req.getVerified() != null) {
      user.setVerified(req.getVerified());
    }

    if (req.getPassword() != null && !req.getPassword().isBlank()) {
      user.setPassword(passwordEncoder.encode(req.getPassword()));
    }

    if (req.getAccountStatus() != null && user.getRole() != Role.ROLE_ADMIN) {
      UserAccountStatus status = switch (req.getAccountStatus()) {
        case "active" -> UserAccountStatus.active;
        case "pending" -> UserAccountStatus.pending;
        case "suspended" -> UserAccountStatus.suspended;
        default -> throw new IllegalArgumentException("Invalid status");
      };

      user.setAccountStatus(status);
    }

    userRepository.save(user);

    User fresh = userRepository.findByIdWithProfiles(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    return userMapper.toAdminUserDto(fresh);
  }

  @Transactional
  public AdminUserDto updateStatus(Long userId, UserStatusRequest req) {

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (user.getRole() == Role.ROLE_ADMIN) {
      throw new IllegalArgumentException("Cannot modify admin");
    }

    UserAccountStatus status = switch (req.getStatus()) {
      case "active" -> UserAccountStatus.active;
      case "pending" -> UserAccountStatus.pending;
      case "suspended" -> UserAccountStatus.suspended;
      default -> throw new IllegalArgumentException("Invalid status");
    };

    user.setAccountStatus(status);

    if ("active".equals(req.getStatus())) {
      user.setVerified(true);
    }

    userRepository.save(user);

    User fresh = userRepository.findByIdWithProfiles(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    return userMapper.toAdminUserDto(fresh);
  }

  @Transactional
  public void deleteUser(Long userId) {

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (user.getRole() == Role.ROLE_ADMIN) {
      throw new IllegalArgumentException("Cannot delete admin");
    }

    userRepository.delete(user);
  }

  @Transactional(readOnly = true)
  public List<EventDto> allEvents() {
    return platformEventRepository.findAll()
            .stream()
            .map(this::toEventDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<ReservationDto> allReservations() {
    return reservationRepository.findAll()
            .stream()
            .map(this::toReservationDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<SolidarityDto> allSolidarity() {
    return solidarityAssociationRepository.findAll()
            .stream()
            .map(this::toSolidarityDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<WalletTransactionDto> allWalletTransactions() {
    return walletTransactionRepository.findAll()
            .stream()
            .map(this::toWalletDto)
            .collect(Collectors.toList());
  }

  private EventDto toEventDto(PlatformEvent event) {
    return EventDto.builder()
            .title(event.getTitle())
            .date(event.getEventDate().toString())
            .location(event.getLocation())
            .participants(event.getParticipants())
            .status(event.getStatus().name())
            .type(event.getTypeLabel())
            .build();
  }

  private ReservationDto toReservationDto(Reservation reservation) {
    return ReservationDto.builder()
            .id("RES-" + String.format("%03d", reservation.getId()))
            .type(reservation.getTypeLabel())
            .item(reservation.getItem())
            .company(reservation.getCompanyName())
            .from(reservation.getFromDate().toString())
            .to(reservation.getToDate().toString())
            .price(reservation.getPrice())
            .status(reservation.getStatus().name())
            .build();
  }

  private SolidarityDto toSolidarityDto(SolidarityAssociation solidarity) {
    return SolidarityDto.builder()
            .id(solidarity.getId())
            .name(solidarity.getName())
            .mission(solidarity.getMission())
            .members(solidarity.getMembers())
            .donations(solidarity.getDonations())
            .status(solidarity.getStatusLabel())
            .ai(solidarity.getAiInsight())
            .build();
  }

  private WalletTransactionDto toWalletDto(WalletTransaction transaction) {
    return WalletTransactionDto.builder()
            .id("TXN-" + transaction.getId())
            .label(transaction.getLabel())
            .type(transaction.getTypeLabel())
            .amount(transaction.getAmount())
            .positive(transaction.getPositiveFlag())
            .status(transaction.getStatus().name())
            .date(transaction.getValueDate() != null
                    ? transaction.getValueDate().toString()
                    : "")
            .from(transaction.getFromParty())
            .to(transaction.getToParty())
            .build();
  }
}