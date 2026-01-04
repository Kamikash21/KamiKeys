package com.kamiplugins.kamikeys.manager;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class KeyManager {

    private final Main plugin;
    private final Random random = new Random();
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public KeyManager(Main plugin) {
        this.plugin = plugin;
    }

    public String generateKey(String type) {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("Types." + type)) {
            return null; // Tipo inválido
        }

        int length = config.getInt("Types." + type + ".Length", 15);
        if (length <= 0) length = 15;

        StringBuilder key = new StringBuilder();
        for (int i = 0; i < length; i++) {
            key.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return key.toString();
    }

    public boolean saveKey(String key, String type, String origin, String exclusiveFor, String generator) {
        FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();

        // Evita sobrescrever
        if (keysConfig.contains("keys." + key)) {
            return false;
        }

        keysConfig.set("keys." + key + ".tipo", type);
        keysConfig.set("keys." + key + ".origem", origin);
        if (exclusiveFor != null && !exclusiveFor.isEmpty()) {
            keysConfig.set("keys." + key + ".exclusivo_para", exclusiveFor);
        }
        keysConfig.set("keys." + key + ".gerado_em", System.currentTimeMillis() / 1000);
        keysConfig.set("keys." + key + ".gerador", generator);

        plugin.getConfigManager().saveKeys();
        return true;
    }

    public boolean keyExists(String key) {
        return plugin.getConfigManager().getKeysConfig().contains("keys." + key);
    }

    public String getKeyType(String key) {
        return plugin.getConfigManager().getKeysConfig().getString("keys." + key + ".tipo", null);
    }

    public String getKeyOrigin(String key) {
        return plugin.getConfigManager().getKeysConfig().getString("keys." + key + ".origem", null);
    }

    public String getExclusiveFor(String key) {
        return plugin.getConfigManager().getKeysConfig().getString("keys." + key + ".exclusivo_para", null);
    }

    public void removeKey(String key) {
        plugin.getConfigManager().getKeysConfig().set("keys." + key, null);
        plugin.getConfigManager().saveKeys();
    }

    // Utilitário: normaliza key (maiúsculas e remove espaços)
    public String normalizeKey(String input) {
        if (input == null) return null;
        String normalized = input.trim().toUpperCase();
        if (!plugin.getConfig().getBoolean("Settings.Case-Sensitive-Keys", false)) {
            return normalized;
        }
        return input.trim();
    }

    // Em KeyManager.java
    public boolean isPlayerKnown(String playerName) {
        return Bukkit.getOfflinePlayer(playerName).hasPlayedBefore();
    }

    public String getPlayerUUID(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return player.getUniqueId().toString();
    }

    public boolean saveExclusiveKey(String key, String type, String playerName, String playerUUID, String generator) {
        FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();

        if (keysConfig.contains("keys." + key)) {
            return false;
        }

        // Salva com dados completos
        keysConfig.set("keys." + key + ".tipo", type);
        keysConfig.set("keys." + key + ".origem", "exclusiva");
        keysConfig.set("keys." + key + ".exclusivo_para.nome", playerName);
        keysConfig.set("keys." + key + ".exclusivo_para.uuid", playerUUID);
        keysConfig.set("keys." + key + ".gerado_em", System.currentTimeMillis() / 1000);
        keysConfig.set("keys." + key + ".gerador", generator);

        plugin.getConfigManager().saveKeys();
        return true;
    }
}