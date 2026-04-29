package com.marketplace.backend.dto;

public class ScanRequest {
    private String barcode;
    private int realQty;
    private String realCondition;
    private String realLocation;

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public int getRealQty() { return realQty; }
    public void setRealQty(int realQty) { this.realQty = realQty; }

    public String getRealCondition() { return realCondition; }
    public void setRealCondition(String realCondition) { this.realCondition = realCondition; }

    public String getRealLocation() { return realLocation; }
    public void setRealLocation(String realLocation) { this.realLocation = realLocation; }
}