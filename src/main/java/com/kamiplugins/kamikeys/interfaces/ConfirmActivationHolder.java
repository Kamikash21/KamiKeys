package com.kamiplugins.kamikeys.interfaces;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ConfirmActivationHolder implements InventoryHolder {
    private final String keyCode;

    public ConfirmActivationHolder(String keyCode) {
        this.keyCode = keyCode;
    }

    public String getKeyCode() {
        return keyCode;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}