package com.kamiplugins.kamikeys.storage.database;

import com.kamiplugins.kamikeys.Main;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseConnection {
    private final Main plugin;
    private Connection connection;
    private final String url;
    private final String username;
    private final String password;
    private final boolean isEnabled;

    public DatabaseConnection(Main plugin) {
        this.plugin = plugin;

        // VERIFICAR SE DATABASE ESTÁ HABILITADO NA CONFIGURAÇÃO
        this.isEnabled = plugin.getConfig().getBoolean("Database.Enabled", false);

        if (!isEnabled) {
            plugin.getLogger().info("Database está desabilitado, usando YAML como fallback.");
            this.url = null;
            this.username = null;
            this.password = null;
            return;
        }

        String host = plugin.getConfig().getString("Database.Host", "localhost");
        int port = plugin.getConfig().getInt("Database.Port", 3306);
        String database = plugin.getConfig().getString("Database.Database", "kamikeys");
        this.username = plugin.getConfig().getString("Database.Username", "root");
        this.password = plugin.getConfig().getString("Database.Password", "");
        String type = plugin.getConfig().getString("Database.Type", "mysql");

        if ("mysql".equals(type) || "mariadb".equals(type)) {
            this.url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false";
        } else if ("sqlite".equals(type)) {
            this.url = "jdbc:sqlite:" + plugin.getDataFolder() + "/database.db";
        } else {
            throw new IllegalArgumentException("Tipo de banco de dados não suportado: " + type);
        }

        // SÓ CONECTAR SE ESTIVER HABILITADO
        if (isEnabled) {
            connect();
        } else {
            this.connection = null;
        }
    }

    public void connect() {
        if (!isEnabled) {
            return; // NÃO CONECTAR SE NÃO ESTIVER HABILITADO
        }

        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            connection = DriverManager.getConnection(url, username, password);
            plugin.getLogger().info("Conectado ao banco de dados com sucesso!");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao conectar ao banco de dados", e);
            // NÃO LANÇAR EXCEPTION - DEIXAR O SISTEMA CONTINUAR COM YAML
        }
    }

    public Connection getConnection() throws SQLException {
        if (!isEnabled) {
            throw new SQLException("Database não está habilitado");
        }

        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    public void close() {
        if (!isEnabled) {
            return; // NÃO FECHAR SE NÃO ESTIVER HABILITADO
        }

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Conexão com banco de dados fechada.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao fechar conexão com banco de dados", e);
        }
    }

    public boolean isConnected() {
        if (!isEnabled) {
            return false;
        }

        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}