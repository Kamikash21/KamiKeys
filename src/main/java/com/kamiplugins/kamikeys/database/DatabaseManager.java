package com.kamiplugins.kamikeys.database;

import com.kamiplugins.kamikeys.Main;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseManager {

    private final Main plugin;
    private HikariDataSource dataSource;
    private String databaseType;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        this.databaseType = plugin.getConfig().getString("Database.Type", "SQLITE").toUpperCase();
        connect();
        createTable();
    }

    private void connect() {
        HikariConfig config = new HikariConfig();

        if (databaseType.equals("MYSQL")) {
            // LINHAS MODIFICADAS PARA COMPATIBILIDADE DE AUTENTICAÇÃO
            String jdbcUrl = "jdbc:mysql://" +
                    plugin.getConfig().getString("Database.Host") + ":" +
                    plugin.getConfig().getString("Database.Port") + "/" +
                    plugin.getConfig().getString("Database.Name") +
                    "?enabledTLSProtocols=TLSv1.2&allowPublicKeyRetrieval=true&useSSL=false";

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(plugin.getConfig().getString("Database.User"));
            config.setPassword(plugin.getConfig().getString("Database.Pass"));
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            plugin.getLogger().info("Conectando ao MySQL com parâmetros de compatibilidade...");
        } else if (databaseType.equals("SQLITE")) {
            config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/keys.db");
            plugin.getLogger().info("Usando SQLite (arquivo local)...");
        } else {
            plugin.getLogger().severe("Tipo de banco de dados inválido no config.yml!");
            return;
        }

        try {
            this.dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            plugin.getLogger().severe("Falha na conexão com o banco de dados: " + e.getMessage());
        }
    }

    // ... (O restante do seu código da classe DatabaseManager permanece o mesmo) ...
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Conexão com o banco de dados fechada.");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS KamiKeys (" +
                "key_code VARCHAR(255) PRIMARY KEY, " +
                "key_type VARCHAR(255) NOT NULL, " +
                "generated_by VARCHAR(255) NOT NULL, " +
                "generated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "redeemed_by VARCHAR(255) NULL, " +
                "redeemed_date TIMESTAMP NULL" +
                ");";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
            plugin.getLogger().info("Tabela KamiKeys verificada/criada.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao criar tabela: ", e);
        }
    }

    // Método para salvar uma nova key gerada
    public void saveKey(String key, String type, String admin) {
        String sql = "INSERT INTO KamiKeys (key_code, key_type, generated_by) VALUES (?, ?, ?)";

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, key);
                stmt.setString(2, type);
                stmt.setString(3, admin);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao salvar key no banco!", e);
            }
        });
    }

    // Método para validar e ativar uma key (Deleta após o uso conforme solicitado)
    public void activateKey(String key, String playerName, java.util.function.Consumer<String> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String checkSql = "SELECT key_type FROM KamiKeys WHERE key_code = ? AND redeemed_by IS NULL";
            String deleteSql = "DELETE FROM KamiKeys WHERE key_code = ?";

            try (Connection conn = getConnection()) {
                // Verifica se a key existe
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, key);
                    ResultSet rs = checkStmt.executeQuery();

                    if (rs.next()) {
                        String type = rs.getString("key_type");

                        // Exclui a key após ativação para não ser usada de novo
                        try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                            delStmt.setString(1, key);
                            delStmt.executeUpdate();
                        }

                        // Retorna o tipo da key para a thread principal processar os comandos
                        plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(type));
                    } else {
                        plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao processar ativação de key!", e);
            }
        });
    }

    // Método para listar todas as keys de um tipo específico (Pendente de resgate)
    public void getActiveKeys(String type, java.util.function.Consumer<java.util.List<String>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT key_code FROM KamiKeys WHERE key_type = ?";
            java.util.List<String> keys = new java.util.ArrayList<>();

            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, type);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    keys.add(rs.getString("key_code"));
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(keys));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao listar keys!", e);
            }
        });
    }

    // Apaga todas as keys de um tipo específico
    public void deleteAllKeysByType(String type) {
        String sql = "DELETE FROM KamiKeys WHERE key_code = ?"; // Deleta as pendentes
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, type);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao apagar keys do tipo " + type, e);
            }
        });
    }
}
