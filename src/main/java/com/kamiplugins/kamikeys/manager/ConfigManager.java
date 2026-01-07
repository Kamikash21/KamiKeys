package com.kamiplugins.kamikeys.manager;

import com.kamiplugins.kamikeys.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final Main plugin;
    private FileConfiguration keysConfig;
    private File logsFile;
    private File keysFile;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig(); // gera config.yml se não existir
        setupFiles();
    }

    private void setupFiles() {
        // Cria pasta de logs
        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        // Caminho do arquivo de logs (do config.yml)
        String logPath = plugin.getConfig().getString("Logs.FilePath", "plugins/KamiKeys/logs/keys.log");
        this.logsFile = new File(logPath);

        if (!logsFile.exists()) {
            try {
                logsFile.getParentFile().mkdirs();
                logsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Não foi possível criar o arquivo de logs: " + logsFile.getAbsolutePath());
                e.printStackTrace();
            }
        }

        // Carrega ou cria keys.yml
        this.keysFile = new File(plugin.getDataFolder(), "keys.yml"); // ← INICIALIZA O ATRIBUTO!
        if (!this.keysFile.exists()) {
            try {
                this.keysFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Não foi possível criar keys.yml");
                e.printStackTrace();
            }
        }
        this.keysConfig = YamlConfiguration.loadConfiguration(this.keysFile); // ← usa o atributo
    }

    public FileConfiguration getKeysConfig() {
        return keysConfig;
    }

    public void saveKeys() {
        File keysFile = new File(plugin.getDataFolder(), "keys.yml");
        try {
            keysConfig.save(keysFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar keys.yml");
            e.printStackTrace();
        }
    }

    public File getLogsFile() {
        return logsFile;
    }

    public void reloadAll() {
        // Recarrega config.yml (cria se não existir)
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        // Recarrega ou cria keys.yml
        File keysFile = new File(plugin.getDataFolder(), "keys.yml");
        if (!keysFile.exists()) {
            try {
                keysFile.getParentFile().mkdirs(); // Garante pasta KamiKeys/
                keysFile.createNewFile(); // Cria keys.yml vazio
                plugin.getLogger().info("Arquivo keys.yml não encontrado, criando padrão...");
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao criar keys.yml: " + e.getMessage());
            }
        }
        this.keysConfig = YamlConfiguration.loadConfiguration(keysFile);

        // Recarrega logs e outros arquivos (se tiver)
        setupFiles();
    }

    public File getKeysFile() {
        return keysFile;
    }

    public File getConfigFile() {
        return new File(plugin.getDataFolder(), "config.yml");
    }
}