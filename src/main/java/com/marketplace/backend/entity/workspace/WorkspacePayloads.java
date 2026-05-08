package com.marketplace.backend.entity.workspace;

import java.math.BigDecimal;

public final class WorkspacePayloads {

  private WorkspacePayloads() {}

  public record OrderLineItem(String label, Integer quantity, BigDecimal unitPrice) {}

  public record TrackingStep(String label, String timestamp, boolean done) {}
}
