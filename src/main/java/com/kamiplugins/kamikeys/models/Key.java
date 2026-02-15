package com.kamiplugins.kamikeys.models;

import com.kamiplugins.kamikeys.models.enums.KeyOrigin;
import com.kamiplugins.kamikeys.models.enums.KeyType;
import com.kamiplugins.kamikeys.models.enums.KeyState;

import java.util.UUID;

public class Key {
    private final UUID internalId;
    private final String code;
    private final KeyOrigin origin;
    private final String typeKey; // String dinâmica - fonte da verdade
    private final KeyType typeEnum; // Enum para compatibilidade apenas
    private KeyState state;
    private final String generatedBy;
    private final String createdAt;
    private String exclusiveToUuid;
    private String exclusiveToName;
    private String activatedBy;
    private String activatedAt;
    private String activatedByUuid;

    public Key(UUID internalId, String code, KeyOrigin origin, String typeKey, KeyType typeEnum, KeyState state, String generatedBy, String createdAt) {
        this.internalId = internalId;
        this.code = code;
        this.origin = origin;
        this.typeKey = typeKey;
        this.typeEnum = typeEnum;
        this.state = state;
        this.generatedBy = generatedBy;
        this.createdAt = createdAt;
    }

    // Construtor para compatibilidade com enum
    public Key(UUID internalId, String code, KeyOrigin origin, KeyType type, KeyState state, String generatedBy, String createdAt) {
        this(internalId, code, origin, type.name(), type, state, generatedBy, createdAt);
    }

    // Construtor para tipo dinâmico
    public Key(UUID internalId, String code, KeyOrigin origin, String typeKey, KeyState state, String generatedBy, String createdAt) {
        // Converter para enum ou usar DEFAULT para compatibilidade
        KeyType typeEnum;
        try {
            typeEnum = KeyType.valueOf(typeKey.toUpperCase());
        } catch (IllegalArgumentException e) {
            typeEnum = KeyType.BASICA; // Valor padrão para tipos dinâmicos
        }
        this.internalId = internalId;
        this.code = code;
        this.origin = origin;
        this.typeKey = typeKey;
        this.typeEnum = typeEnum;
        this.state = state;
        this.generatedBy = generatedBy;
        this.createdAt = createdAt;
    }

    // Getters e Setters
    public UUID getInternalId() { return internalId; }
    public String getCode() { return code; }
    public KeyOrigin getOrigin() { return origin; }
    public String getTypeKey() { return typeKey; } // Getter para type dinâmico - fonte da verdade
    public KeyType getType() { return typeEnum; } // Getter para compatibilidade
    public KeyState getState() { return state; }
    public void setState(KeyState state) { this.state = state; }
    public String getGeneratedBy() { return generatedBy; }
    public String getCreatedAt() { return createdAt; }
    public String getExclusiveToUuid() { return exclusiveToUuid; }
    public void setExclusiveToUuid(String exclusiveToUuid) { this.exclusiveToUuid = exclusiveToUuid; }
    public String getExclusiveToName() { return exclusiveToName; }
    public void setExclusiveToName(String exclusiveToName) { this.exclusiveToName = exclusiveToName; }
    public String getActivatedBy() { return activatedBy; }
    public void setActivatedBy(String activatedBy) { this.activatedBy = activatedBy; }
    public String getActivatedAt() { return activatedAt; }
    public void setActivatedAt(String activatedAt) { this.activatedAt = activatedAt; }
    public String getActivatedByUuid() { return activatedByUuid; }
    public void setActivatedByUuid(String activatedByUuid) { this.activatedByUuid = activatedByUuid; }
}