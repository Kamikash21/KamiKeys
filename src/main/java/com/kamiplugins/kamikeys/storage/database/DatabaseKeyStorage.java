package com.kamiplugins.kamikeys.storage.database;

import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.models.enums.KeyOrigin;
import com.kamiplugins.kamikeys.models.enums.KeyState;
import com.kamiplugins.kamikeys.models.enums.KeyType;
import com.kamiplugins.kamikeys.repositories.KeyRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DatabaseKeyStorage implements KeyRepository {
    private final DatabaseConnection dbConnection;

    public DatabaseKeyStorage(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
        initializeTable();
    }

    private void initializeTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS `keys` (
                internal_id CHAR(36) PRIMARY KEY,
                code VARCHAR(50) UNIQUE NOT NULL,
                origin VARCHAR(20) NOT NULL,
                type VARCHAR(20) NOT NULL, -- Guardar como string dinâmica
                state VARCHAR(20) NOT NULL,
                generated_by VARCHAR(100) NOT NULL,
                created_at VARCHAR(30) NOT NULL,
                exclusive_to_uuid CHAR(36),
                exclusive_to_name VARCHAR(100),
                activated_by VARCHAR(100),
                activated_at VARCHAR(30),
                activated_by_uuid CHAR(36)
            )
        """;

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar tabela de keys: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void save(Key key) {
        String sql = """
            INSERT INTO `keys` (internal_id, code, origin, type, state, generated_by, created_at, 
                               exclusive_to_uuid, exclusive_to_name, activated_by, activated_at, activated_by_uuid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false); // Iniciar transação

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, key.getInternalId().toString());
                stmt.setString(2, key.getCode());
                stmt.setString(3, key.getOrigin().name());
                stmt.setString(4, key.getTypeKey()); // Salvar como string dinâmica
                stmt.setString(5, key.getState().name());
                stmt.setString(6, key.getGeneratedBy());
                stmt.setString(7, key.getCreatedAt());
                stmt.setString(8, key.getExclusiveToUuid());
                stmt.setString(9, key.getExclusiveToName());
                stmt.setString(10, key.getActivatedBy());
                stmt.setString(11, key.getActivatedAt());
                stmt.setString(12, key.getActivatedByUuid());

                stmt.executeUpdate();
                conn.commit(); // Confirmar transação
            }
        } catch (SQLException e) {
            System.err.println("Erro ao salvar key: " + e.getMessage());
            e.printStackTrace();
            try {
                Connection conn = dbConnection.getConnection();
                if (conn != null && !conn.isClosed()) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("Erro ao fazer rollback: " + rollbackEx.getMessage());
            }
        }
    }

    @Override
    public Optional<Key> findByCode(String code) {
        String sql = "SELECT * FROM `keys` WHERE code = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                UUID internalId = UUID.fromString(rs.getString("internal_id"));
                KeyOrigin origin = KeyOrigin.valueOf(rs.getString("origin"));
                String typeKey = rs.getString("type"); // Pegar como string dinâmica
                KeyState state = KeyState.valueOf(rs.getString("state"));

                // Converter para enum apenas para compatibilidade
                KeyType typeEnum;
                try {
                    typeEnum = KeyType.valueOf(typeKey.toUpperCase());
                } catch (IllegalArgumentException e) {
                    typeEnum = KeyType.BASICA; // Valor padrão para tipos dinâmicos
                }

                Key key = new Key(
                        internalId,
                        rs.getString("code"),
                        origin,
                        typeKey, // Passar a string dinâmica como fonte da verdade
                        typeEnum,
                        state,
                        rs.getString("generated_by"),
                        rs.getString("created_at")
                );

                key.setExclusiveToUuid(rs.getString("exclusive_to_uuid"));
                key.setExclusiveToName(rs.getString("exclusive_to_name"));
                key.setActivatedBy(rs.getString("activated_by"));
                key.setActivatedAt(rs.getString("activated_at"));
                key.setActivatedByUuid(rs.getString("activated_by_uuid"));

                return Optional.of(key);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar key por código: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public List<Key> findAll() {
        List<Key> result = new ArrayList<>();
        String sql = "SELECT * FROM `keys`";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID internalId = UUID.fromString(rs.getString("internal_id"));
                KeyOrigin origin = KeyOrigin.valueOf(rs.getString("origin"));
                String typeKey = rs.getString("type"); // Pegar como string dinâmica
                KeyState state = KeyState.valueOf(rs.getString("state"));

                // Converter para enum apenas para compatibilidade
                KeyType typeEnum;
                try {
                    typeEnum = KeyType.valueOf(typeKey.toUpperCase());
                } catch (IllegalArgumentException e) {
                    typeEnum = KeyType.BASICA; // Valor padrão para tipos dinâmicos
                }

                Key key = new Key(
                        internalId,
                        rs.getString("code"),
                        origin,
                        typeKey, // Passar a string dinâmica como fonte da verdade
                        typeEnum,
                        state,
                        rs.getString("generated_by"),
                        rs.getString("created_at")
                );

                key.setExclusiveToUuid(rs.getString("exclusive_to_uuid"));
                key.setExclusiveToName(rs.getString("exclusive_to_name"));
                key.setActivatedBy(rs.getString("activated_by"));
                key.setActivatedAt(rs.getString("activated_at"));
                key.setActivatedByUuid(rs.getString("activated_by_uuid"));

                result.add(key);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar todas as keys: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public List<Key> findByOrigin(String origin) {
        List<Key> result = new ArrayList<>();
        String sql = "SELECT * FROM `keys` WHERE origin = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, origin.toUpperCase());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UUID internalId = UUID.fromString(rs.getString("internal_id"));
                KeyOrigin keyOrigin = KeyOrigin.valueOf(rs.getString("origin"));
                String typeKey = rs.getString("type"); // Pegar como string dinâmica
                KeyState state = KeyState.valueOf(rs.getString("state"));

                // Converter para enum apenas para compatibilidade
                KeyType typeEnum;
                try {
                    typeEnum = KeyType.valueOf(typeKey.toUpperCase());
                } catch (IllegalArgumentException e) {
                    typeEnum = KeyType.BASICA; // Valor padrão para tipos dinâmicos
                }

                Key key = new Key(
                        internalId,
                        rs.getString("code"),
                        keyOrigin,
                        typeKey, // Passar a string dinâmica como fonte da verdade
                        typeEnum,
                        state,
                        rs.getString("generated_by"),
                        rs.getString("created_at")
                );

                key.setExclusiveToUuid(rs.getString("exclusive_to_uuid"));
                key.setExclusiveToName(rs.getString("exclusive_to_name"));
                key.setActivatedBy(rs.getString("activated_by"));
                key.setActivatedAt(rs.getString("activated_at"));
                key.setActivatedByUuid(rs.getString("activated_by_uuid"));

                result.add(key);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar keys por origem: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public List<Key> findByType(String type) {
        List<Key> result = new ArrayList<>();
        String sql = "SELECT * FROM `keys` WHERE type = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type); // Comparar com string dinâmica
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UUID internalId = UUID.fromString(rs.getString("internal_id"));
                KeyOrigin origin = KeyOrigin.valueOf(rs.getString("origin"));
                String typeKey = rs.getString("type"); // Pegar como string dinâmica
                KeyState state = KeyState.valueOf(rs.getString("state"));

                // Converter para enum apenas para compatibilidade
                KeyType typeEnum;
                try {
                    typeEnum = KeyType.valueOf(typeKey.toUpperCase());
                } catch (IllegalArgumentException e) {
                    typeEnum = KeyType.BASICA; // Valor padrão para tipos dinâmicos
                }

                Key key = new Key(
                        internalId,
                        rs.getString("code"),
                        origin,
                        typeKey, // Passar a string dinâmica como fonte da verdade
                        typeEnum,
                        state,
                        rs.getString("generated_by"),
                        rs.getString("created_at")
                );

                key.setExclusiveToUuid(rs.getString("exclusive_to_uuid"));
                key.setExclusiveToName(rs.getString("exclusive_to_name"));
                key.setActivatedBy(rs.getString("activated_by"));
                key.setActivatedAt(rs.getString("activated_at"));
                key.setActivatedByUuid(rs.getString("activated_by_uuid"));

                result.add(key);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar keys por tipo: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public List<Key> findByExclusiveToName(String playerName) {
        List<Key> result = new ArrayList<>();
        String sql = "SELECT * FROM `keys` WHERE exclusive_to_name = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UUID internalId = UUID.fromString(rs.getString("internal_id"));
                KeyOrigin origin = KeyOrigin.valueOf(rs.getString("origin"));
                String typeKey = rs.getString("type");
                KeyState state = KeyState.valueOf(rs.getString("state"));

                // Converter para enum apenas para compatibilidade
                KeyType typeEnum;
                try {
                    typeEnum = KeyType.valueOf(typeKey.toUpperCase());
                } catch (IllegalArgumentException e) {
                    typeEnum = KeyType.BASICA; // Valor padrão para tipos dinâmicos
                }

                Key key = new Key(
                        internalId,
                        rs.getString("code"),
                        origin,
                        typeKey,
                        typeEnum,
                        state,
                        rs.getString("generated_by"),
                        rs.getString("created_at")
                );

                key.setExclusiveToUuid(rs.getString("exclusive_to_uuid"));
                key.setExclusiveToName(rs.getString("exclusive_to_name"));
                key.setActivatedBy(rs.getString("activated_by"));
                key.setActivatedAt(rs.getString("activated_at"));
                key.setActivatedByUuid(rs.getString("activated_by_uuid"));

                result.add(key);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar keys por nome exclusivo: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public void update(Key key) {
        String sql = """
            UPDATE `keys` SET origin = ?, type = ?, state = ?, generated_by = ?, created_at = ?, 
                          exclusive_to_uuid = ?, exclusive_to_name = ?, activated_by = ?, 
                          activated_at = ?, activated_by_uuid = ?
            WHERE code = ?
        """;

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false); // Iniciar transação

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, key.getOrigin().name());
                stmt.setString(2, key.getTypeKey()); // Atualizar com string dinâmica
                stmt.setString(3, key.getState().name());
                stmt.setString(4, key.getGeneratedBy());
                stmt.setString(5, key.getCreatedAt());
                stmt.setString(6, key.getExclusiveToUuid());
                stmt.setString(7, key.getExclusiveToName());
                stmt.setString(8, key.getActivatedBy());
                stmt.setString(9, key.getActivatedAt());
                stmt.setString(10, key.getActivatedByUuid());
                stmt.setString(11, key.getCode());

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    System.err.println("Tentativa de atualizar key inexistente: " + key.getCode());
                }
                conn.commit(); // Confirmar transação
            }
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar key: " + e.getMessage());
            e.printStackTrace();
            try {
                Connection conn = dbConnection.getConnection();
                if (conn != null && !conn.isClosed()) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("Erro ao fazer rollback: " + rollbackEx.getMessage());
            }
        }
    }

    @Override
    public void delete(String code) {
        String sql = "DELETE FROM `keys` WHERE code = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao deletar key: " + e.getMessage());
            e.printStackTrace();
        }
    }
}