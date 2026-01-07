package com.kamiplugins.kamikeys;

import com.kamiplugins.kamikeys.admin.AdminBaseGUI;
import com.kamiplugins.kamikeys.admin.AdminMainMenuListener;
import com.kamiplugins.kamikeys.admin.AdminSubmenuListener;
import com.kamiplugins.kamikeys.commands.AtivarCommand;
import com.kamiplugins.kamikeys.commands.KamikeysCommand;
import com.kamiplugins.kamikeys.commands.PlayerKeysCommand;
import com.kamiplugins.kamikeys.gui.PlayerKeysGUI;
import com.kamiplugins.kamikeys.listeners.AdminGUIListener;
import com.kamiplugins.kamikeys.listeners.PlayerKeysGUIListener;
import com.kamiplugins.kamikeys.manager.ConfigManager;
import com.kamiplugins.kamikeys.manager.KeyManager;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin {

    private ConfigManager configManager;
    private KeyManager keyManager;
    private final Map<UUID, PlayerKeysGUI> playerKeysGUIs = new HashMap<>();
    private final Map<UUID, AdminBaseGUI> adminKeysGUIs = new HashMap<>();

    // Getter para GUIs do jogador
    public Map<UUID, PlayerKeysGUI> getPlayerKeysGUIs() {
        return playerKeysGUIs;
    }

    // Getter para GUIs do admin
    public Map<UUID, AdminBaseGUI> getAdminKeysGUIs() {
        return adminKeysGUIs;
    }

    @Override
    public void onEnable() {
        // Carrega configuração e cria arquivos
        this.configManager = new ConfigManager(this);
        this.keyManager = new KeyManager(this);

        // Registra comandos
        getCommand("kamikeys").setExecutor(new KamikeysCommand(this));
        getCommand("ativar").setExecutor(new AtivarCommand(this));
        getCommand("keys").setExecutor(new PlayerKeysCommand(this));

        // Registra listeners
        new PlayerKeysGUIListener(this);
        new AdminMainMenuListener(this);
        new AdminSubmenuListener(this);// ← CORRETO!

        getServer().getConsoleSender().sendMessage(ColorUtils.translate("&b[KamiKeys] &aPlugin iniciado com sucesso! (v1.5)"));
    }

    @Override
    public void onDisable() {
        if (configManager != null) {
            configManager.saveKeys();
        }
        getServer().getConsoleSender().sendMessage(ColorUtils.translate("&b[KamiKeys] &cPlugin desligado."));
    }

    // Getters
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public KeyManager getKeyManager() {
        return keyManager;
    }
}