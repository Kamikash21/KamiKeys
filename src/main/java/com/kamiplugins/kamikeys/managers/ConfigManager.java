package com.kamiplugins.kamikeys.managers;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.storage.yaml.YAMLConfigStorage;
import com.kamiplugins.kamikeys.utils.UxService;

public class ConfigManager {
    private final Main plugin;
    private final YAMLConfigStorage yamlStorage;
    private final UxService uxService;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.yamlStorage = new YAMLConfigStorage(plugin);
        this.uxService = new UxService(plugin);
    }

    public void reload() {
        yamlStorage.reloadConfig();
        uxService.reloadMessages();
    }

    public org.bukkit.configuration.file.FileConfiguration getConfig() {
        return yamlStorage.getConfig();
    }

    public UxService getUxService() {
        return uxService;
    }
}