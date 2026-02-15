package com.kamiplugins.kamikeys.storage.yaml;

import com.kamiplugins.kamikeys.models.Voucher;
import com.kamiplugins.kamikeys.repositories.VoucherRepository;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class YAMLVoucherStorage implements VoucherRepository {
    private final File file;
    private FileConfiguration config;

    public YAMLVoucherStorage(File file) {
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void save(Voucher voucher) {
        String voucherId = voucher.getInternalId().toString();

        config.set("vouchers." + voucherId + ".linked_key", voucher.getLinkedKeyCode());
        config.set("vouchers." + voucherId + ".owner_uuid", voucher.getOwnerUuid());
        config.set("vouchers." + voucherId + ".owner_name", voucher.getOwnerName());
        config.set("vouchers." + voucherId + ".expiration_time", voucher.getExpirationTime());
        config.set("vouchers." + voucherId + ".active", voucher.isActive());
        config.set("vouchers." + voucherId + ".created_by", voucher.getCreatedBy());
        config.set("vouchers." + voucherId + ".created_at", voucher.getCreatedAt());

        saveToFile();
    }

    @Override
    public Optional<Voucher> findById(String id) {
        if (!config.contains("vouchers." + id)) {
            return Optional.empty();
        }

        String linkedKey = config.getString("vouchers." + id + ".linked_key");
        String ownerUuid = config.getString("vouchers." + id + ".owner_uuid");
        String ownerName = config.getString("vouchers." + id + ".owner_name");
        long expirationTime = config.getLong("vouchers." + id + ".expiration_time");
        boolean active = config.getBoolean("vouchers." + id + ".active", true);
        String createdBy = config.getString("vouchers." + id + ".created_by", "sistema");
        String createdAt = config.getString("vouchers." + id + ".created_at", getCurrentDateTime());

        Voucher voucher = new Voucher(
                UUID.fromString(id), // Converter ID de string para UUID
                linkedKey,
                ownerUuid,
                ownerName,
                expirationTime,
                createdBy,
                createdAt
        );

        voucher.setActive(active);

        return Optional.of(voucher);
    }

    @Override
    public Optional<Voucher> findByLinkedKeyCode(String keyCode) {
        if (!config.contains("vouchers")) {
            return Optional.empty();
        }

        for (String voucherId : config.getConfigurationSection("vouchers").getKeys(false)) {
            String linkedKey = config.getString("vouchers." + voucherId + ".linked_key");

            if (linkedKey != null && linkedKey.equalsIgnoreCase(keyCode)) {
                return findById(voucherId);
            }
        }

        return Optional.empty();
    }



    @Override
    public List<Voucher> findAll() {
        List<Voucher> result = new ArrayList<>();
        if (!config.contains("vouchers")) {
            return result;
        }

        for (String voucherId : config.getConfigurationSection("vouchers").getKeys(false)) {
            Optional<Voucher> voucherOpt = findById(voucherId);
            if (voucherOpt.isPresent()) {
                result.add(voucherOpt.get());
            }
        }
        return result;
    }

    @Override
    public List<Voucher> findByOwner(String ownerUuid) {
        List<Voucher> result = new ArrayList<>();
        if (!config.contains("vouchers")) {
            return result;
        }

        for (String voucherId : config.getConfigurationSection("vouchers").getKeys(false)) {
            String voucherOwner = config.getString("vouchers." + voucherId + ".owner_uuid");
            if (ownerUuid.equals(voucherOwner)) {
                Optional<Voucher> voucherOpt = findById(voucherId);
                if (voucherOpt.isPresent()) {
                    result.add(voucherOpt.get());
                }
            }
        }
        return result;
    }

    @Override
    public void update(Voucher voucher) {
        String voucherId = voucher.getInternalId().toString();
        if (!config.contains("vouchers." + voucherId)) {
            System.err.println("Tentativa de atualizar voucher inexistente: " + voucherId);
            return;
        }

        config.set("vouchers." + voucherId + ".linked_key", voucher.getLinkedKeyCode());
        config.set("vouchers." + voucherId + ".owner_uuid", voucher.getOwnerUuid());
        config.set("vouchers." + voucherId + ".owner_name", voucher.getOwnerName());
        config.set("vouchers." + voucherId + ".expiration_time", voucher.getExpirationTime());
        config.set("vouchers." + voucherId + ".active", voucher.isActive());
        config.set("vouchers." + voucherId + ".created_by", voucher.getCreatedBy());
        config.set("vouchers." + voucherId + ".created_at", voucher.getCreatedAt());

        saveToFile();
    }

    @Override
    public void delete(String id) {
        if (config.contains("vouchers." + id)) {
            config.set("vouchers." + id, null); // ← CORRIGIDO: Remove do arquivo
            saveToFile();
        }
    }

    private void saveToFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.err.println("Erro ao salvar YAML vouchers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}