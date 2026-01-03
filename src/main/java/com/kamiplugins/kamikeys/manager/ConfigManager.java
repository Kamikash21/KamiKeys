package com.kamiplugins.kamikeys.manager;

import com.kamiplugins.kamikeys.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final Main plugin;
    private File keysFile;
    private FileConfiguration keysConfig;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig(); // Cria o config.yml padrão
        createKeysFile();
    }

    private void createKeysFile() {
        keysFile = new File(plugin.getDataFolder(), "keys.yml");
        if (!keysFile.exists()) {
            try {
                // Cria o arquivo keys.yml vazio se ele não existir
                keysFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                plugin.getLogger().severe("Não foi possível criar o arquivo keys.yml!");
            }
        }
        keysConfig = YamlConfiguration.loadConfiguration(keysFile);
    }

    // Retorna a configuração do arquivo keys.yml para leitura
    public FileConfiguration getKeysConfig() {
        return keysConfig;
    }

    // Salva o conteúdo do buffer de memória para o arquivo keys.yml
    public void saveKeys() {
        try {
            keysConfig.save(keysFile);
        } catch (IOException e) {
            e.printStackTrace();
            plugin.getLogger().severe("Não foi possível salvar as chaves no keys.yml!");
        }
    }

    // Recarrega as configurações da memória para o arquivo
    public void reloadConfigs() {
        plugin.reloadConfig(); // Recarrega o config.yml principal
        keysConfig = YamlConfiguration.loadConfiguration(keysFile); // Recarrega o keys.yml
    }
}