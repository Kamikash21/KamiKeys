package com.kamiplugins.kamikeys.utils;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import java.util.HashMap;
import java.util.Map;

public class KeyData implements ConfigurationSerializable {

    private String key;
    private String proprietario_uuid;
    private String nick;
    private String gerador;

    // Construtor necessário para criar novos objetos em runtime
    public KeyData(String key, String proprietario_uuid, String nick, String gerador) {
        this.key = key;
        this.proprietario_uuid = proprietario_uuid;
        this.nick = nick;
        this.gerador = gerador;
    }

    // Método obrigatório para serializar (salvar no YAML)
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", key);
        map.put("proprietario_uuid", proprietario_uuid);
        map.put("nick", nick);
        map.put("gerador", gerador);
        return map;
    }

    // Método estático obrigatório para deserializar (ler do YAML de volta para objeto)
    public static KeyData deserialize(Map<String, Object> map) {
        // Garantir que todos os campos existem antes de acessar
        String key = (String) map.getOrDefault("key", "N/A");
        String uuid = (String) map.getOrDefault("proprietario_uuid", "N/A");
        String nick = (String) map.getOrDefault("nick", "N/A");
        String gerador = (String) map.getOrDefault("gerador", "N/A");

        return new KeyData(key, uuid, nick, gerador);
    }

    // Adicione getters se precisar acessar esses valores em outras classes
    // public String getKey() { return key; } ...
}
