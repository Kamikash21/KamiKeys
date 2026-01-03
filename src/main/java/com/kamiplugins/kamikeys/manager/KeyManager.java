package com.kamiplugins.kamikeys.manager;

import com.kamiplugins.kamikeys.Main;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.bukkit.configuration.ConfigurationSection;

public class KeyManager {

    private final Main plugin;
    private final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    public KeyManager(Main plugin) {
        this.plugin = plugin;
    }

    // Método auxiliar para encontrar o nome real da chave no config.yml (ex: "Basica")
    private String findConfigKeyIgnoreCase(String typeName) {
        ConfigurationSection typesSection = plugin.getConfig().getConfigurationSection("Types");
        if (typesSection == null) { return null; }
        for (String key : typesSection.getKeys(false)) {
            if (key.equalsIgnoreCase(typeName)) {
                return key; // Retorna o nome real (ex: "Basica")
            }
        }
        return null;
    }

    /**
     * Gera uma key aleatória baseada no tipo solicitado com comprimento configurável
     */
    public String generateRandomKey(String type) {
        // 1. Encontra o nome real da chave no config.yml (ignorando case)
        String configKeyName = findConfigKeyIgnoreCase(type);

        int length = 15; // Padrão
        // 2. Se encontrou, pega o Length configurado
        if (configKeyName != null) {
            length = plugin.getConfig().getInt("Types." + configKeyName + ".Length", 15);
        }

        // 3. Gera a chave com o comprimento dinâmico
        return IntStream.range(0, length)
                .map(i -> characters.charAt(random.nextInt(characters.length())))
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.joining());
    }

    /**
     * Retorna o prefixo configurado para o tipo de chave
     */
    public String getPrefixForType(String tipo) {
        // 1. Encontra o nome real da chave no config.yml (ignorando case)
        String configKeyName = findConfigKeyIgnoreCase(tipo);

        if (configKeyName != null) {
            // 2. Se encontrou, tenta buscar o Prefix configurado
            String prefix = plugin.getConfig().getString("Types." + configKeyName + ".Prefix", "");
            if (!prefix.isEmpty()) {
                return prefix;
            }
        }

        // 3. Fallback: Se nada foi encontrado na config, usa este padrão:
        switch (tipo.toLowerCase()) {
            case "basica": return "&a[BASICA]";
            case "comum": return "&7[COMUM]";
            case "rara": return "&b[RARA]";
            default: return "&f[" + tipo.toUpperCase() + "]";
        }
    }

    /**
     * Processa a criação de múltiplas keys e salva no banco (Método que faltava no snippet anterior)
     */
    public void createKeys(String type, int amount, String adminName) {
        for (int i = 0; i < amount; i++) {
            String newKey = generateRandomKey(type);
            plugin.getDatabaseManager().saveKey(newKey, type.toLowerCase(), adminName);
        }
    }
}
