package com.kamiplugins.kamikeys.services;

import com.kamiplugins.kamikeys.Main;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActivationCooldownService {

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Main plugin;

    public ActivationCooldownService(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Verifica se o jogador está em cooldown
     * Staff com permissão kamikeys.admin.staff têm bypass
     */
    public boolean isInCooldown(Player player) {
        // Verificar se o jogador é staff (tem bypass)
        if (player.hasPermission("kamikeys.admin.staff")) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        Long cooldownEnd = cooldowns.get(playerId);

        if (cooldownEnd == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime >= cooldownEnd) {
            // Cooldown expirou, remove o registro
            cooldowns.remove(playerId);
            return false;
        }

        return true;
    }

    /**
     * Obtém os segundos restantes no cooldown
     * Staff com permissão kamikeys.admin.staff têm bypass
     */
    public long getRemainingSeconds(Player player) {
        // Staff com bypass não tem cooldown
        if (player.hasPermission("kamikeys.admin.staff")) {
            return 0L;
        }

        UUID playerId = player.getUniqueId();
        Long cooldownEnd = cooldowns.get(playerId);

        if (cooldownEnd == null) {
            return 0L;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime >= cooldownEnd) {
            // Cooldown expirou, remove o registro
            cooldowns.remove(playerId);
            return 0L;
        }

        long remainingMillis = cooldownEnd - currentTime;
        // Arredonda para cima para garantir que o usuário espere o tempo completo
        return (long) Math.ceil(remainingMillis / 1000.0);
    }

    /**
     * Registra um cooldown para o jogador
     * Staff com permissão kamikeys.admin.staff não recebem cooldown
     */
    public void register(Player player) {
        // Não aplicar cooldown para staff com bypass
        if (player.hasPermission("kamikeys.admin.staff")) {
            return;
        }

        int cooldownSeconds = plugin.getConfig().getInt("Security.ActivationCooldownSeconds", 0);
        if (cooldownSeconds <= 0) {
            return; // Não aplica cooldown se estiver desativado
        }

        long cooldownEnd = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        cooldowns.put(player.getUniqueId(), cooldownEnd);
    }

    /**
     * Remove o cooldown de um jogador (opcional, útil para testes ou admin)
     */
    public void removeCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    /**
     * Limpa todos os cooldowns (útil para limpeza geral)
     */
    public void clearAllCooldowns() {
        cooldowns.clear();
    }
}