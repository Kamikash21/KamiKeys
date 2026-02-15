package com.kamiplugins.kamikeys.storage.yaml;

import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.models.enums.KeyOrigin;
import com.kamiplugins.kamikeys.models.enums.KeyState;
import com.kamiplugins.kamikeys.models.enums.KeyType;
import com.kamiplugins.kamikeys.repositories.KeyRepository;
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

public class YAMLKeyStorage implements KeyRepository {
    private final File file;
    private FileConfiguration config;

    public YAMLKeyStorage(File file) {
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void save(Key key) {
        config.set("keys." + key.getCode() + ".tipo", key.getTypeKey()); // Salvar como string dinâmica
        config.set("keys." + key.getCode() + ".origem", key.getOrigin().name());
        config.set("keys." + key.getCode() + ".estado", key.getState().name());
        config.set("keys." + key.getCode() + ".gerado_por", key.getGeneratedBy());
        config.set("keys." + key.getCode() + ".data_geracao", key.getCreatedAt());

        // Salvar dados exclusivos se aplicável
        if (key.getExclusiveToName() != null) {
            config.set("keys." + key.getCode() + ".exclusivo_para.nome", key.getExclusiveToName());
        }
        if (key.getExclusiveToUuid() != null) {
            config.set("keys." + key.getCode() + ".exclusivo_para.uuid", key.getExclusiveToUuid());
        }
        if (key.getActivatedBy() != null) {
            config.set("keys." + key.getCode() + ".ativado_por.nome", key.getActivatedBy());
        }
        if (key.getActivatedAt() != null) {
            config.set("keys." + key.getCode() + ".ativado_em", key.getActivatedAt());
        }
        if (key.getActivatedByUuid() != null) {
            config.set("keys." + key.getCode() + ".ativado_por.uuid", key.getActivatedByUuid());
        }

        saveToFile();
    }

    @Override
    public Optional<Key> findByCode(String code) {
        if (!config.contains("keys." + code)) {
            return Optional.empty();
        }

        String type = config.getString("keys." + code + ".tipo", "desconhecido"); // Pegar como string dinâmica
        String origin = config.getString("keys." + code + ".origem", "desconhecido");
        String state = config.getString("keys." + code + ".estado", "ATIVA");
        String generatedBy = config.getString("keys." + code + ".gerado_por", "sistema");
        String createdAt = config.getString("keys." + code + ".data_geracao", getCurrentDateTime());

        // Converter para enum apenas para compatibilidade (não como fonte da verdade)
        KeyType typeEnum;
        try {
            typeEnum = KeyType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            typeEnum = KeyType.BASICA; // Valor padrão para tipos dinâmicos
        }

        KeyOrigin originEnum = KeyOrigin.valueOf(origin.toUpperCase());
        KeyState stateEnum = KeyState.valueOf(state.toUpperCase());

        Key key = new Key(
                UUID.randomUUID(),
                code,
                originEnum,
                type, // Passar a string dinâmica como fonte da verdade
                stateEnum,
                generatedBy,
                createdAt
        );

        String exclusiveName = config.getString("keys." + code + ".exclusivo_para.nome");
        String exclusiveUuid = config.getString("keys." + code + ".exclusivo_para.uuid");
        if (exclusiveName != null) {
            key.setExclusiveToName(exclusiveName);
        }
        if (exclusiveUuid != null) {
            key.setExclusiveToUuid(exclusiveUuid);
        }

        String activatedBy = config.getString("keys." + code + ".ativado_por.nome");
        String activatedAt = config.getString("keys." + code + ".ativado_em");
        String activatedByUuid = config.getString("keys." + code + ".ativado_por.uuid");
        if (activatedBy != null) {
            key.setActivatedBy(activatedBy);
        }
        if (activatedAt != null) {
            key.setActivatedAt(activatedAt);
        }
        if (activatedByUuid != null) {
            key.setActivatedByUuid(activatedByUuid);
        }

        return Optional.of(key);
    }

    @Override
    public List<Key> findAll() {
        List<Key> result = new ArrayList<>();
        if (!config.contains("keys")) {
            return result;
        }

        for (String keyCode : config.getConfigurationSection("keys").getKeys(false)) {
            Optional<Key> keyOpt = findByCode(keyCode);
            if (keyOpt.isPresent()) {
                result.add(keyOpt.get());
            }
        }
        return result;
    }

    @Override
    public List<Key> findByOrigin(String origin) {
        List<Key> result = new ArrayList<>();
        if (!config.contains("keys")) {
            return result;
        }

        for (String keyCode : config.getConfigurationSection("keys").getKeys(false)) {
            String keyOrigin = config.getString("keys." + keyCode + ".origem", "");
            if (origin.equalsIgnoreCase(keyOrigin)) {
                Optional<Key> keyOpt = findByCode(keyCode);
                if (keyOpt.isPresent()) {
                    result.add(keyOpt.get());
                }
            }
        }
        return result;
    }

    @Override
    public List<Key> findByType(String type) {
        List<Key> result = new ArrayList<>();
        if (!config.contains("keys")) {
            return result;
        }

        for (String keyCode : config.getConfigurationSection("keys").getKeys(false)) {
            String keyType = config.getString("keys." + keyCode + ".tipo", ""); // Comparar com string dinâmica
            if (type.equalsIgnoreCase(keyType)) {
                Optional<Key> keyOpt = findByCode(keyCode);
                if (keyOpt.isPresent()) {
                    result.add(keyOpt.get());
                }
            }
        }
        return result;
    }

    @Override
    public List<Key> findByExclusiveToName(String playerName) {
        List<Key> result = new ArrayList<>();
        if (!config.contains("keys")) {
            return result;
        }

        for (String keyCode : config.getConfigurationSection("keys").getKeys(false)) {
            String exclusiveName = config.getString("keys." + keyCode + ".exclusivo_para.nome");
            if (playerName.equalsIgnoreCase(exclusiveName)) {
                Optional<Key> keyOpt = findByCode(keyCode);
                if (keyOpt.isPresent()) {
                    result.add(keyOpt.get());
                }
            }
        }
        return result;
    }

    @Override
    public void update(Key key) {
        if (!config.contains("keys." + key.getCode())) {
            System.err.println("Tentativa de atualizar key inexistente: " + key.getCode());
            return;
        }

        config.set("keys." + key.getCode() + ".tipo", key.getTypeKey()); // Atualizar com string dinâmica
        config.set("keys." + key.getCode() + ".origem", key.getOrigin().name());
        config.set("keys." + key.getCode() + ".estado", key.getState().name());
        config.set("keys." + key.getCode() + ".gerado_por", key.getGeneratedBy());
        config.set("keys." + key.getCode() + ".data_geracao", key.getCreatedAt());

        if (key.getExclusiveToName() != null && !key.getExclusiveToName().isEmpty()) {
            config.set("keys." + key.getCode() + ".exclusivo_para.nome", key.getExclusiveToName());
        } else {
            config.set("keys." + key.getCode() + ".exclusivo_para.nome", null);
        }

        if (key.getExclusiveToUuid() != null && !key.getExclusiveToUuid().isEmpty()) {
            config.set("keys." + key.getCode() + ".exclusivo_para.uuid", key.getExclusiveToUuid());
        } else {
            config.set("keys." + key.getCode() + ".exclusivo_para.uuid", null);
        }

        if (key.getActivatedBy() != null && !key.getActivatedBy().isEmpty()) {
            config.set("keys." + key.getCode() + ".ativado_por.nome", key.getActivatedBy());
        } else {
            config.set("keys." + key.getCode() + ".ativado_por.nome", null);
        }

        if (key.getActivatedAt() != null && !key.getActivatedAt().isEmpty()) {
            config.set("keys." + key.getCode() + ".ativado_em", key.getActivatedAt());
        } else {
            config.set("keys." + key.getCode() + ".ativado_em", null);
        }

        if (key.getActivatedByUuid() != null && !key.getActivatedByUuid().isEmpty()) {
            config.set("keys." + key.getCode() + ".ativado_por.uuid", key.getActivatedByUuid());
        } else {
            config.set("keys." + key.getCode() + ".ativado_por.uuid", null);
        }

        saveToFile();
    }

    @Override
    public void delete(String code) {
        if (config.contains("keys." + code)) {
            config.set("keys." + code, null);
            saveToFile();
        }
    }

    private void saveToFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.err.println("Erro ao salvar YAML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}