package com.kamiplugins.kamikeys.utils;

import com.kamiplugins.kamikeys.Main;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PendingActionManager {
    private final Main plugin;
    private final Map<UUID, PendingAction> pendingActions = new HashMap<>();

    public PendingActionManager(Main plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    public static class PendingAction {
        private final UUID executor;
        private final String actionType;
        private final String target;
        private final String filter;
        private final long createdAt;

        public PendingAction(UUID executor, String actionType, String target, String filter) {
            this.executor = executor;
            this.actionType = actionType;
            this.target = target;
            this.filter = filter;
            this.createdAt = System.currentTimeMillis();
        }

        public UUID getExecutor() { return executor; }
        public String getActionType() { return actionType; }
        public String getTarget() { return target; }
        public String getFilter() { return filter; }
        public long getCreatedAt() { return createdAt; }
    }

    public void createAction(UUID executor, String actionType, String target, String filter) {
        // Cancelar ação anterior se existir
        if (pendingActions.containsKey(executor)) {
            cancelAction(executor);
        }

        pendingActions.put(executor, new PendingAction(executor, actionType, target, filter));
    }

    public PendingAction getPendingAction(UUID executor) {
        PendingAction action = pendingActions.get(executor);
        if (action != null) {
            long expirationTime = plugin.getConfig().getLong("Confirmation.ExpirationSeconds", 30) * 1000;
            if (System.currentTimeMillis() - action.getCreatedAt() > expirationTime) {
                // Ação expirou
                cancelAction(executor);
                return null;
            }
        }
        return action;
    }

    public boolean hasPendingAction(UUID executor) {
        return getPendingAction(executor) != null;
    }

    public void confirmAction(UUID executor) {
        PendingAction action = getPendingAction(executor);
        if (action != null) {
            // Remover a ação
            pendingActions.remove(executor);
        }
    }

    public void cancelAction(UUID executor) {
        pendingActions.remove(executor);
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long expirationTime = plugin.getConfig().getLong("Confirmation.ExpirationSeconds", 30) * 1000;
                pendingActions.entrySet().removeIf(entry -> {
                    PendingAction action = entry.getValue();
                    return System.currentTimeMillis() - action.getCreatedAt() > expirationTime;
                });
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L); // Verificar a cada segundo
    }
}