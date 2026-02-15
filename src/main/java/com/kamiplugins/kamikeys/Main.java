package com.kamiplugins.kamikeys;

import com.kamiplugins.kamikeys.commands.AtivarCommand;
import com.kamiplugins.kamikeys.commands.KamiKeysCommand;
import com.kamiplugins.kamikeys.commands.PlayerKeysCommand;
import com.kamiplugins.kamikeys.gui.AdminVoucherKeySelectionGUI;
import com.kamiplugins.kamikeys.gui.PlayerVoucherKeySelectionGUI;
import com.kamiplugins.kamikeys.gui.VoucherConfirmationGUI;
import com.kamiplugins.kamikeys.listeners.*;
import com.kamiplugins.kamikeys.managers.ConfigManager;
import com.kamiplugins.kamikeys.managers.KeyManager;
import com.kamiplugins.kamikeys.managers.AuditLogger;
import com.kamiplugins.kamikeys.managers.BackupManager;
import com.kamiplugins.kamikeys.managers.LogRotationManager;
import com.kamiplugins.kamikeys.services.ActivationCooldownService;
import com.kamiplugins.kamikeys.services.KeyService;
import com.kamiplugins.kamikeys.services.VoucherService;
import com.kamiplugins.kamikeys.services.ValidationService;
import com.kamiplugins.kamikeys.storage.StorageManager;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import com.kamiplugins.kamikeys.utils.ConsoleColorUtils;
import com.kamiplugins.kamikeys.utils.PendingActionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin {
    private ConfigManager configManager;
    private KeyManager keyManager;
    private AuditLogger auditLogger;
    private BackupManager backupManager;
    private LogRotationManager logRotationManager;
    private StorageManager storageManager;

    // Serviços
    private KeyService keyService;
    private VoucherService voucherService;
    private ValidationService validationService;
    private ActivationCooldownService activationCooldownService; // Novo serviço

    // Managers de GUI
    private final Map<UUID, Object> playerKeysGUIs = new HashMap<>();
    private final Map<UUID, Object> adminKeysGUIs = new HashMap<>();
    private final Map<UUID, AdminVoucherKeySelectionGUI> adminVoucherGUIs = new HashMap<>();
    private final Map<UUID, PlayerVoucherKeySelectionGUI> playerVoucherGUIs = new HashMap<>();
    private final Map<UUID, Boolean> pendingVoucherExpiry = new HashMap<>();
    private final Map<UUID, VoucherConfirmationGUI> voucherConfirmationGUIs = new HashMap<>();


    // Manager de ações pendentes
    private PendingActionManager pendingActionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        this.configManager = new ConfigManager(this);
        this.storageManager = new StorageManager(this);

        this.keyService = new KeyService(this, storageManager.getKeyRepository());
        this.voucherService = new VoucherService(this, storageManager.getVoucherRepository(), keyService);
        this.validationService = new ValidationService(this);
        this.activationCooldownService = new ActivationCooldownService(this); // Inicializar novo serviço

        this.pendingActionManager = new PendingActionManager(this);

        this.keyManager = new KeyManager(this);
        this.auditLogger = new AuditLogger(this);
        this.backupManager = new BackupManager(this);
        this.logRotationManager = new LogRotationManager(this);
        this.logRotationManager.scheduleAutoRotation();


        new VoucherExpiryChatListener(this);
        new VoucherInteractListener(this, voucherService);




        registerCommands();
        registerListeners();

        getLogger().info(ConsoleColorUtils.colorize(
                ConsoleColorUtils.CYAN + "╔══════════════════════════════════════════════════════════════╗" +
                        ConsoleColorUtils.RESET
        ));
        getLogger().info(ConsoleColorUtils.colorize(
                ConsoleColorUtils.CYAN + "║ " +
                        ConsoleColorUtils.GREEN + ConsoleColorUtils.BOLD + "KamiKeys" +
                        ConsoleColorUtils.WHITE + " v2.0 " +
                        ConsoleColorUtils.YELLOW + "•" +
                        ConsoleColorUtils.WHITE + " Sistema de Keys & Vouchers" +
                        ConsoleColorUtils.CYAN + "                   ║" +
                        ConsoleColorUtils.RESET
        ));
        getLogger().info(ConsoleColorUtils.colorize(
                ConsoleColorUtils.CYAN + "║ " +
                        ConsoleColorUtils.WHITE + "Status: " +
                        ConsoleColorUtils.GREEN + ConsoleColorUtils.BOLD + "ONLINE" +
                        ConsoleColorUtils.WHITE + "  |  " +
                        ConsoleColorUtils.WHITE + "Build: " +
                        ConsoleColorUtils.YELLOW + getDescription().getVersion() +
                        ConsoleColorUtils.WHITE + "  |  " +
                        ConsoleColorUtils.WHITE + "Autor: " +
                        ConsoleColorUtils.BLUE + "Kamikash" +
                        ConsoleColorUtils.CYAN + "          ║" +
                        ConsoleColorUtils.RESET
        ));
        getLogger().info(ConsoleColorUtils.colorize(
                ConsoleColorUtils.CYAN + "╚══════════════════════════════════════════════════════════════╝" +
                        ConsoleColorUtils.RESET
        ));

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                for (ItemStack it : p.getInventory().getContents()) {
                    if (it != null) {
                        voucherService.refreshVoucherLore(it);
                    }
                }
            }
        }, 20L * 10, 20L * 10); // 10s




    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.shutdown();
        }
        getLogger().info(ConsoleColorUtils.colorize(
                ConsoleColorUtils.RED + "KamiKeys foi desativado." +
                        ConsoleColorUtils.RESET
        ));
    }

    private void registerCommands() {
        getCommand("ativar").setExecutor(new AtivarCommand(this, keyService, validationService, activationCooldownService));
        getCommand("kamikeys").setExecutor(new KamiKeysCommand(this));
        getCommand("keys").setExecutor(new PlayerKeysCommand(this)); // Passar o plugin inteiro
    }

    private void registerListeners() {
        new PlayerKeysGUIListener(this, new PlayerKeysCommand(this), keyService); // Novo listener
        new AdminSubmenuListener(this, keyService, voucherService, validationService);
        new PlayerVoucherKeySelectionListener(this, getKeyService());
        new VoucherLoreRefreshListener(this, voucherService);


    }

    public ConfigManager getConfigManager() { return configManager; }
    public KeyManager getKeyManager() { return keyManager; }
    public AuditLogger getAuditLogger() { return auditLogger; }
    public BackupManager getBackupManager() { return backupManager; }
    public LogRotationManager getLogRotationManager() { return logRotationManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public KeyService getKeyService() { return keyService; }
    public VoucherService getVoucherService() { return voucherService; }
    public ValidationService getValidationService() { return validationService; }
    public ActivationCooldownService getActivationCooldownService() { return activationCooldownService; }
    public Map<UUID, Object> getPlayerKeysGUIs() { return playerKeysGUIs; } // Getter para GUIs
    public Map<UUID, Object> getAdminKeysGUIs() { return adminKeysGUIs; }
    public PendingActionManager getPendingActionManager() { return pendingActionManager; }
    public Map<UUID, AdminVoucherKeySelectionGUI> getAdminVoucherGUIs() { return adminVoucherGUIs; }
    public Map<UUID, PlayerVoucherKeySelectionGUI> getPlayerVoucherGUIs() { return playerVoucherGUIs; }
    public Map<UUID, Boolean> getPendingVoucherExpiry() { return pendingVoucherExpiry; }
    public Map<UUID, VoucherConfirmationGUI> getVoucherConfirmationGUIs() { return voucherConfirmationGUIs; }

}
