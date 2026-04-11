package com.marketplace.backend.service;

import com.marketplace.backend.dto.CreateListingRequest;
import com.marketplace.backend.dto.ExchangeRequestDto;
import com.marketplace.backend.dto.ListingDto;
import com.marketplace.backend.dto.ListingModerationRequest;
import com.marketplace.backend.dto.ReservationDto;
import com.marketplace.backend.dto.WalletTransactionDto;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.ExchangeRequest;
import com.marketplace.backend.entity.Listing;
import com.marketplace.backend.entity.Reservation;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.WalletTransaction;
import com.marketplace.backend.entity.enums.ExchangeRequestStatus;
import com.marketplace.backend.entity.enums.ListingStatus;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.ExchangeRequestRepository;
import com.marketplace.backend.repository.ListingRepository;
import com.marketplace.backend.repository.ReservationRepository;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.repository.WalletTransactionRepository;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListingService {

  private final ListingRepository listingRepository;
  private final UserRepository userRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final ExchangeRequestRepository exchangeRequestRepository;
  private final ReservationRepository reservationRepository;
  private final WalletTransactionRepository walletTransactionRepository;

  @Transactional(readOnly = true)
  public List<ListingDto> findAllPublished(Authentication auth) {
    return listingRepository.findAll().stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<ListingDto> findMine(Authentication auth) {
    Enterprise e = requireEnterprise(auth);
    return listingRepository.findByEnterpriseId(e.getId()).stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public ListingDto create(Authentication auth, CreateListingRequest req) {
    Enterprise e = requireEnterprise(auth);
    Listing l =
        Listing.builder()
            .enterprise(e)
            .title(req.getTitle())
            .category(req.getCategory())
            .price(req.getPrice())
            .quantityLabel(req.getQuantityLabel())
            .status(ListingStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)))
            .aiInsight(req.getAiInsight())
            .build();
    listingRepository.save(l);
    e.setListingsCount((e.getListingsCount() != null ? e.getListingsCount() : 0) + 1);
    enterpriseRepository.save(e);
    return toDto(l);
  }

  @Transactional
  public ListingDto updateMine(Long id, Authentication auth, CreateListingRequest req) {
    Enterprise e = requireEnterprise(auth);
    Listing l =
        listingRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));
    if (!l.getEnterprise().getId().equals(e.getId())) {
      throw new IllegalArgumentException("Forbidden");
    }
    l.setTitle(req.getTitle());
    l.setCategory(req.getCategory());
    l.setPrice(req.getPrice());
    l.setQuantityLabel(req.getQuantityLabel());
    l.setStatus(ListingStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    l.setAiInsight(req.getAiInsight());
    listingRepository.save(l);
    return toDto(l);
  }

  @Transactional
  public void deleteMine(Long id, Authentication auth) {
    Enterprise e = requireEnterprise(auth);
    Listing l =
        listingRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));
    if (!l.getEnterprise().getId().equals(e.getId())) {
      throw new IllegalArgumentException("Forbidden");
    }
    listingRepository.delete(l);
  }

  @Transactional
  public ListingDto moderate(Long id, ListingModerationRequest req) {
    Listing l =
        listingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    l.setStatus(ListingStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    listingRepository.save(l);
    return toDto(l);
  }


  private Enterprise requireEnterprise(Authentication auth) {
    User u =
        userRepository
            .findByEmailWithProfiles(auth.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (u.getRole() == Role.ROLE_ADMIN) {
      return enterpriseRepository
          .findAll()
          .stream()
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("No enterprise in system"));
    }
    if (u.getEnterprise() == null) {
      throw new IllegalArgumentException("Enterprise profile required");
    }
    return u.getEnterprise();
  }

  private ListingDto toDto(Listing l) {
    String company =
        l.getEnterprise() != null ? l.getEnterprise().getCompanyName() : "";
    return ListingDto.builder()
        .id(l.getId())
        .title(l.getTitle())
        .company(company)
        .category(l.getCategory())
        .price(l.getPrice())
        .qty(l.getQuantityLabel())
        .status(l.getStatus().name())
        .ai(l.getAiInsight())
        .views(l.getViews())
        .enquiries(l.getEnquiries())
        .posted(l.getPostedLabel())
        .build();
  }


  @Transactional(readOnly = true)
  public List<ExchangeRequestDto> myExchangeRequests(Authentication auth) {
    Enterprise e = requireEnterprise(auth);
    return exchangeRequestRepository.findByRecipientEnterpriseId(e.getId()).stream()
        .map(this::toExchangeDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public ExchangeRequestDto updateExchangeStatus(Long id, String status, Authentication auth) {
    User u =
        userRepository
            .findByEmailWithProfiles(auth.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    ExchangeRequest r =
        exchangeRequestRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));
    if (u.getRole() != Role.ROLE_ADMIN) {
      Enterprise e = u.getEnterprise();
      if (e == null || !r.getRecipientEnterprise().getId().equals(e.getId())) {
        throw new IllegalArgumentException("Forbidden");
      }
    }
    r.setStatus(ExchangeRequestStatus.valueOf(status.toLowerCase(java.util.Locale.ROOT)));
    exchangeRequestRepository.save(r);
    return toExchangeDto(r);
  }

  @Transactional(readOnly = true)
  public List<ReservationDto> myReservations(Authentication auth) {
    Enterprise e = requireEnterprise(auth);
    return reservationRepository.findByEnterpriseId(e.getId()).stream()
        .map(this::toReservationDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<WalletTransactionDto> myWalletTransactions(Authentication auth) {
    User u =
        userRepository
            .findByEmailWithProfiles(auth.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(u.getId()).stream()
        .map(this::toWalletDto)
        .collect(Collectors.toList());
  }

  private ExchangeRequestDto toExchangeDto(ExchangeRequest r) {
    return ExchangeRequestDto.builder()
        .id("REQ-" + String.format("%03d", r.getId()))
        .from(r.getFromCompanyName())
        .avatar(r.getFromAvatar())
        .item(r.getItem())
        .type(r.getTypeLabel())
        .from_date(r.getFromDate().toString())
        .to_date(r.getToDate().toString())
        .duration(r.getDurationLabel())
        .price(r.getPrice())
        .message(r.getMessage())
        .status(r.getStatus().name())
        .received(r.getReceivedLabel())
        .urgent(r.isUrgent())
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
