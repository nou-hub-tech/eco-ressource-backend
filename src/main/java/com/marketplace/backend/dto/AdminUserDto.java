package com.marketplace.backend.dto;


import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.Transporter;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.UserAccountStatus;
import java.time.format.DateTimeFormatter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {


  private static final DateTimeFormatter JOINED =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");


  private String id;
  private String name;
  private String email;
  private String company;
  private String role;
  private String status;
  private String phone;
  private String city;
  private String joined;
  private int listings;
  private int orders;
  private String revenue;
  private boolean verified;
  private String avatar;

  public static AdminUserDto from(User u) {
    String role =
        u.getRole() == Role.ROLE_ENTERPRISE
            ? "enterprise"
            : u.getRole() == Role.ROLE_TRANSPORTER ? "transporter" : "enterprise";
    if (u.getRole() == Role.ROLE_ADMIN) {
      role = "enterprise";
    }
    String company = "";
    int listings = 0;
    int orders = 0;
    String revenue = "0";
    if (u.getEnterprise() != null) {
      Enterprise e = u.getEnterprise();
      company = e.getCompanyName();
      listings = e.getListingsCount() != null ? e.getListingsCount() : 0;
      orders = e.getOrdersCount() != null ? e.getOrdersCount() : 0;
      revenue = e.getRevenue() != null ? e.getRevenue() : "0";
    } else if (u.getTransporter() != null) {
      Transporter t = u.getTransporter();
      company = t.getCompanyName();
      listings = t.getListingsCount() != null ? t.getListingsCount() : 0;
      orders = t.getOrdersCount() != null ? t.getOrdersCount() : 0;
      revenue = t.getRevenue() != null ? t.getRevenue() : "0";
    }
    UserAccountStatus st = u.getAccountStatus();
    String status =
        st == null
            ? "active"
            : switch (st) {
              case active -> "active";
              case pending -> "pending";
              case suspended -> "suspended";
            };
    if (u.getRole() == Role.ROLE_ADMIN) {
      company = "Platform";
      status = "active";
    }
    return AdminUserDto.builder()
        .id("USR-" + String.format("%03d", u.getId()))
        .name(u.getFullName())
        .email(u.getEmail())
        .company(company)
        .role(u.getRole() == Role.ROLE_TRANSPORTER ? "transporter" : "enterprise")
        .status(status)
        .phone(u.getPhone() != null ? u.getPhone() : "")
        .city(u.getCity() != null ? u.getCity() : "")
        .joined(u.getCreatedAt() != null ? JOINED.format(u.getCreatedAt()) : "")
        .listings(listings)
        .orders(orders)
        .revenue(revenue)
        .verified(Boolean.TRUE.equals(u.getVerified()))
        .avatar(initials(u.getFullName()))
        .build();
  }

  private static String initials(String fullName) {
    if (fullName == null || fullName.isBlank()) {
      return "?";
    }
    String[] p = fullName.trim().split("\\s+");
    if (p.length == 1) {
      return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
    }
    return ("" + p[0].charAt(0) + p[p.length - 1].charAt(0)).toUpperCase();
  }

}
