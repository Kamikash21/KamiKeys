package com.kamiplugins.kamikeys.models.dtos;

import com.kamiplugins.kamikeys.models.enums.KeyOrigin;
import com.kamiplugins.kamikeys.models.enums.KeyState;
import com.kamiplugins.kamikeys.models.enums.KeyType;

public class KeyDTO {
    private String code;
    private KeyOrigin origin;
    private KeyType type;
    private KeyState state;
    private String generatedBy;
    private String createdAt;
    private String exclusiveToUuid;
    private String exclusiveToName;

    // Construtor padrão
    public KeyDTO() {}

    // Getters e Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public KeyOrigin getOrigin() { return origin; }
    public void setOrigin(KeyOrigin origin) { this.origin = origin; }
    public KeyType getType() { return type; }
    public void setType(KeyType type) { this.type = type; }
    public KeyState getState() { return state; }
    public void setState(KeyState state) { this.state = state; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getExclusiveToUuid() { return exclusiveToUuid; }
    public void setExclusiveToUuid(String exclusiveToUuid) { this.exclusiveToUuid = exclusiveToUuid; }
    public String getExclusiveToName() { return exclusiveToName; }
    public void setExclusiveToName(String exclusiveToName) { this.exclusiveToName = exclusiveToName; }
}