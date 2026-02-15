package com.kamiplugins.kamikeys.storage.database;

import com.kamiplugins.kamikeys.models.Voucher;
import com.kamiplugins.kamikeys.repositories.VoucherRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DatabaseVoucherStorage implements VoucherRepository {
    private final DatabaseConnection dbConnection;

    public DatabaseVoucherStorage(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
        initializeTable();
    }

    private void initializeTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS `vouchers` (
                internal_id VARCHAR(36) PRIMARY KEY,
                linked_key VARCHAR(50) NOT NULL,
                owner_uuid VARCHAR(36),
                owner_name VARCHAR(100),
                expiration_time BIGINT NOT NULL,
                active BOOLEAN DEFAULT TRUE,
                created_by VARCHAR(100) NOT NULL,
                created_at VARCHAR(30) NOT NULL
            )
        """;

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar tabela de vouchers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void save(Voucher voucher) {
        String sql = """
            INSERT INTO `vouchers` (internal_id, linked_key, owner_uuid, owner_name, 
                                 expiration_time, active, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false); // Iniciar transação

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, voucher.getInternalId().toString());
                stmt.setString(2, voucher.getLinkedKeyCode());
                stmt.setString(3, voucher.getOwnerUuid());
                stmt.setString(4, voucher.getOwnerName());
                stmt.setLong(5, voucher.getExpirationTime());
                stmt.setBoolean(6, voucher.isActive());
                stmt.setString(7, voucher.getCreatedBy());
                stmt.setString(8, voucher.getCreatedAt());

                stmt.executeUpdate();
                conn.commit(); // Confirmar transação
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback na mesma conexão
                } catch (SQLException rollbackEx) {
                    System.err.println("Erro ao fazer rollback: " + rollbackEx.getMessage());
                }
            }
            System.err.println("Erro ao salvar voucher: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Restaurar estado
                    conn.close(); // Fechar conexão
                } catch (SQLException e) {
                    System.err.println("Erro ao fechar conexão: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public Optional<Voucher> findById(String id) {
        String sql = "SELECT * FROM `vouchers` WHERE internal_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                UUID internalId = UUID.fromString(rs.getString("internal_id"));

                Voucher voucher = new Voucher(
                        internalId,
                        rs.getString("linked_key"),
                        rs.getString("owner_uuid"),
                        rs.getString("owner_name"),
                        rs.getLong("expiration_time"),
                        rs.getString("created_by"),
                        rs.getString("created_at")
                );

                voucher.setActive(rs.getBoolean("active"));

                return Optional.of(voucher);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar voucher por ID: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Optional<Voucher> findByLinkedKeyCode(String keyCode) {
        String sql = "SELECT * FROM `vouchers` WHERE linked_key = ? LIMIT 1";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, keyCode);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                UUID internalId = UUID.fromString(rs.getString("internal_id"));

                Voucher voucher = new Voucher(
                        internalId,
                        rs.getString("linked_key"),
                        rs.getString("owner_uuid"),
                        rs.getString("owner_name"),
                        rs.getLong("expiration_time"),
                        rs.getString("created_by"),
                        rs.getString("created_at")
                );

                voucher.setActive(rs.getBoolean("active"));
                return Optional.of(voucher);
            }

        } catch (SQLException e) {
            System.err.println("Erro ao buscar voucher por linked_key: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }



    @Override
    public List<Voucher> findAll() {
        List<Voucher> result = new ArrayList<>();
        String sql = "SELECT * FROM `vouchers`";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID internalId = UUID.fromString(rs.getString("internal_id"));

                Voucher voucher = new Voucher(
                        internalId,
                        rs.getString("linked_key"),
                        rs.getString("owner_uuid"),
                        rs.getString("owner_name"),
                        rs.getLong("expiration_time"),
                        rs.getString("created_by"),
                        rs.getString("created_at")
                );

                voucher.setActive(rs.getBoolean("active"));

                result.add(voucher);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar todos os vouchers: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public List<Voucher> findByOwner(String ownerUuid) {
        List<Voucher> result = new ArrayList<>();
        String sql = "SELECT * FROM `vouchers` WHERE owner_uuid = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ownerUuid);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UUID internalId = UUID.fromString(rs.getString("internal_id"));

                Voucher voucher = new Voucher(
                        internalId,
                        rs.getString("linked_key"),
                        rs.getString("owner_uuid"),
                        rs.getString("owner_name"),
                        rs.getLong("expiration_time"),
                        rs.getString("created_by"),
                        rs.getString("created_at")
                );

                voucher.setActive(rs.getBoolean("active"));

                result.add(voucher);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar vouchers por proprietário: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public void update(Voucher voucher) {
        String sql = """
            UPDATE `vouchers` SET linked_key = ?, owner_uuid = ?, owner_name = ?, 
                              expiration_time = ?, active = ?, created_by = ?, created_at = ?
            WHERE internal_id = ?
        """;

        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false); // Iniciar transação

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, voucher.getLinkedKeyCode());
                stmt.setString(2, voucher.getOwnerUuid());
                stmt.setString(3, voucher.getOwnerName());
                stmt.setLong(4, voucher.getExpirationTime());
                stmt.setBoolean(5, voucher.isActive());
                stmt.setString(6, voucher.getCreatedBy());
                stmt.setString(7, voucher.getCreatedAt());
                stmt.setString(8, voucher.getInternalId().toString());

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    System.err.println("Tentativa de atualizar voucher inexistente: " + voucher.getInternalId());
                }
                conn.commit(); // Confirmar transação
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback na mesma conexão
                } catch (SQLException rollbackEx) {
                    System.err.println("Erro ao fazer rollback: " + rollbackEx.getMessage());
                }
            }
            System.err.println("Erro ao atualizar voucher: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Restaurar estado
                    conn.close(); // Fechar conexão
                } catch (SQLException e) {
                    System.err.println("Erro ao fechar conexão: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void delete(String id) {
        String sql = "DELETE FROM `vouchers` WHERE internal_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao deletar voucher: " + e.getMessage());
            e.printStackTrace();
        }
    }
}