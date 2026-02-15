package com.kamiplugins.kamikeys.storage;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.repositories.KeyRepository;
import com.kamiplugins.kamikeys.repositories.VoucherRepository;
import com.kamiplugins.kamikeys.storage.database.DatabaseKeyStorage;
import com.kamiplugins.kamikeys.storage.database.DatabaseVoucherStorage;
import com.kamiplugins.kamikeys.storage.database.DatabaseConnection;
import com.kamiplugins.kamikeys.storage.yaml.YAMLKeyStorage;
import com.kamiplugins.kamikeys.storage.yaml.YAMLVoucherStorage;

import java.io.File;

public class StorageManager {
    private final Main plugin;
    private final DatabaseConnection databaseConnection;
    private final boolean useDatabase;

    private KeyRepository keyRepository;
    private VoucherRepository voucherRepository;

    public StorageManager(Main plugin) {
        this.plugin = plugin;
        this.useDatabase = plugin.getConfig().getBoolean("Database.Enabled", false);

        // INICIALIZAR CONEXÃO APENAS SE FOR USAR DATABASE
        if (useDatabase) {
            this.databaseConnection = new DatabaseConnection(plugin);
            initializeDatabaseStorage();
        } else {
            this.databaseConnection = null;
            initializeYAMLStorage();
        }
    }

    private void initializeYAMLStorage() {
        File keysFile = new File(plugin.getDataFolder(), "keys.yml");
        File vouchersFile = new File(plugin.getDataFolder(), "vouchers.yml");

        this.keyRepository = new YAMLKeyStorage(keysFile);
        this.voucherRepository = new YAMLVoucherStorage(vouchersFile);
    }

    private void initializeDatabaseStorage() {
        this.keyRepository = new DatabaseKeyStorage(databaseConnection);
        this.voucherRepository = new DatabaseVoucherStorage(databaseConnection);
    }

    public KeyRepository getKeyRepository() {
        return keyRepository;
    }

    public VoucherRepository getVoucherRepository() {
        return voucherRepository;
    }

    public boolean isUsingDatabase() {
        return useDatabase;
    }

    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }

    public void shutdown() {
        if (databaseConnection != null) {
            databaseConnection.close();
        }
    }
}