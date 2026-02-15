package com.kamiplugins.kamikeys.models;

import java.util.UUID;

public class Voucher {
    private final UUID internalId;
    private final String linkedKeyCode;
    private final String ownerUuid;
    private final String ownerName;
    private final long expirationTime;
    private boolean active;
    private final String createdBy;
    private final String createdAt;
    private int validityDays;

    public Voucher(UUID internalId, String linkedKeyCode, String ownerUuid, String ownerName, long expirationTime, String createdBy, String createdAt) {
        this.internalId = internalId;
        this.linkedKeyCode = linkedKeyCode;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.expirationTime = expirationTime;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.active = true;
    }

    // Getters e Setters
    public UUID getInternalId() { return internalId; }
    public String getLinkedKeyCode() { return linkedKeyCode; }
    public String getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public long getExpirationTime() { return expirationTime; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCreatedBy() { return createdBy; }
    public String getCreatedAt() { return createdAt; }
    public int getValidityDays() { return validityDays; }
    public  void setValidityDays(int validityDays) { this.validityDays = validityDays; }
}