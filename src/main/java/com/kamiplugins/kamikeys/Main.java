package com.kamiplugins.kamikeys;

import com.kamiplugins.kamikeys.commands.AtivarCommand;
import com.kamiplugins.kamikeys.commands.KamikeysCommand;
import com.kamiplugins.kamikeys.manager.ConfigManager;
import com.kamiplugins.kamikeys.manager.KeyManager;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private ConfigManager configManager;
    private KeyManager keyManager;

    @Override
    public void onEnable() {
        // Carrega configuração e cria arquivos
        this.configManager = new ConfigManager(this);
        this.keyManager = new KeyManager(this);

        // Registra comandos (só os do plugin.yml v1.5)
        getCommand("kamikeys").setExecutor(new KamikeysCommand(this));
        getCommand("ativar").setExecutor(new AtivarCommand(this));
        // getCommand("keys") será adicionado depois

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