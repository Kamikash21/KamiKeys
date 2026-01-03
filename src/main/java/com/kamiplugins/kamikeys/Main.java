package com.kamiplugins.kamikeys;

import com.kamiplugins.kamikeys.commands.AtivarCommand;
import com.kamiplugins.kamikeys.commands.AdminCommands;
import com.kamiplugins.kamikeys.commands.KamikeysCommand;
import com.kamiplugins.kamikeys.commands.GerarKeyCommand;
import com.kamiplugins.kamikeys.database.DatabaseManager;
import com.kamiplugins.kamikeys.manager.ConfigManager;
import com.kamiplugins.kamikeys.manager.KeyManager;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.plugin.java.JavaPlugin;
import com.kamiplugins.kamikeys.commands.GiveKeyCommand;


public class Main extends JavaPlugin {

    // Instância estática para acesso global
    private static Main instance;

    // Gerenciadores do plugin
    private ConfigManager configManager;
    private KeyManager keyManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Inicializa os gerenciadores de configuração e chaves
        this.configManager = new ConfigManager(this);
        this.keyManager = new KeyManager(this);
        this.databaseManager = new DatabaseManager(this);

        // 2. Registra os comandos no servidor Minecraft E O TAB COMPLETE

        ConfigurationSerialization.registerClass(KeyData.class, "KeyData");

        // --- COMANDO /KEY (AdminCommands) ---
        AdminCommands adminExecutor = new AdminCommands(this);
        getCommand("key").setExecutor(adminExecutor);
        getCommand("key").setTabCompleter(adminExecutor);

        // --- COMANDO /ATIVAR (AtivarCommand) ---
        AtivarCommand ativarExecutor = new AtivarCommand(this);
        getCommand("ativar").setExecutor(ativarExecutor);
        getCommand("ativar").setTabCompleter(ativarExecutor);

        // --- OUTROS COMANDOS ---
        // Esses comandos não possuem Tab Completer personalizado, então registramos apenas o Executor
        GerarKeyCommand gerarKeyExecutor = new GerarKeyCommand(this);
        getCommand("gerarkey").setExecutor(gerarKeyExecutor);
        getCommand("gerarkey").setTabCompleter(gerarKeyExecutor); // Adiciona o Tab Completer
        getCommand("kamikeys").setExecutor(new KamikeysCommand());

        // --- ADICIONE O COMANDO /DARKEY AQUI ---
        GiveKeyCommand giveKeyCommand = new GiveKeyCommand(this);
        getCommand("darkey").setExecutor(giveKeyCommand);
        getCommand("darkey").setTabCompleter(giveKeyCommand); // Define o TabCompleter

        // 3. Log de inicialização (usando o utilitário de cores)
        getServer().getConsoleSender().sendMessage(ColorUtils.translate("&b[KamiKeys] &aPlugin iniciado com sucesso! (1.20.6)"));
    }


    @Override
    public void onDisable() {
        // Garante que todas as chaves pendentes sejam salvas ao desligar
        if (configManager != null) {
            configManager.saveKeys();
        }
        // Fecha a conexão com o DB ao desligar
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getServer().getConsoleSender().sendMessage(ColorUtils.translate("&b[KamiKeys] &cPlugin desligado."));
    }

    // Método estático para que outras classes possam chamar Main.getInstance()
    public static Main getInstance() {
        return instance;
    }

    // Getters para acessar os gerenciadores
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public KeyManager getKeyManager() {
        return keyManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
