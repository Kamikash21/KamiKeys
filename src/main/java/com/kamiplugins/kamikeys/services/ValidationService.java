package com.kamiplugins.kamikeys.services;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.models.Key;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ValidationService {
    private final Main plugin;
    private final Map<UUID, Long> lastActivationTimes = new ConcurrentHashMap<>();
    private final int activationCooldownSeconds;

    public ValidationService(Main plugin) {
        this.plugin = plugin;
        this.activationCooldownSeconds = plugin.getConfig().getInt("Security.ActivationCooldownSeconds", 3);
    }

    public boolean canPlayerActivateKey(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastActivation = lastActivationTimes.getOrDefault(playerId, 0L);

        long cooldownMs = activationCooldownSeconds * 1000L;
        return (currentTime - lastActivation) >= cooldownMs;
    }

    public void recordActivationAttempt(Player player) {
        lastActivationTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean isKeyValidForPlayer(Key key, String playerName) {
        if (key.getState() != com.kamiplugins.kamikeys.models.enums.KeyState.ATIVA) {
            return false;
        }

        if (key.getExclusiveToName() != null) {
            return key.getExclusiveToName().equalsIgnoreCase(playerName);
        }

        return true;
    }

    public boolean hasVoucherPermission(Player player, boolean isPlayerVoucher) {
        if (isPlayerVoucher) {
            return player.hasPermission("kamikeys.voucher.player");
        } else {
            return player.hasPermission("kamikeys.voucher.adm");
        }
    }

    public void cleanupOldCooldowns() {
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - (activationCooldownSeconds * 1000L * 2);

        lastActivationTimes.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
    }

    public boolean canPlayerUseKey(Player player, String typeKey) {
        // Por enquanto, vamos retornar true para todos os tipos
        // Esta é uma validação básica que pode ser expandida posteriormente
        return true;
    }

    public boolean hasVoucherPermission(Player player) {
        return player.hasPermission("kamikeys.voucher.player");
    }

}