package com.marketplace.backend.entity.workspace;

public final class WorkspaceEnums {

  private WorkspaceEnums() {}

  public enum ReservationStatus {
    confirmed,
    pending,
    risk,
    cancelled
  }

  public enum ReservationRole {
    provider,
    consumer
  }

  public enum ReservationCategory {
    Machine,
    Space,
    Storage,
    Production
  }

  public enum SlotStatus {
    available,
    balanced,
    peak,
    maintenance
  }

  public enum SlotType {
    Dock,
    Storage,
    Meeting,
    Production
  }

  public enum SlotPortfolio {
    owned,
    partner
  }

  public enum OrderStatus {
    draft,
    processing,
    invoiced,
    fulfilled,
    flagged
  }

  public enum PaymentStatus {
    paid,
    pending,
    review
  }

  public enum NotificationChannel {
    email,
    sms,
    both
  }

  public enum InsightTone {
    info,
    success,
    warning,
    danger
  }
}
