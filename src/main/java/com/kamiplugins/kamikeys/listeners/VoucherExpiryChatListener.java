package com.kamiplugins.kamikeys.listeners;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.gui.AdminVoucherExpirySelectionGUI;
import com.kamiplugins.kamikeys.gui.AdminVoucherKeySelectionGUI;
import com.kamiplugins.kamikeys.gui.VoucherConfirmationGUI;
import com.kamiplugins.kamikeys.utils.UxService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class VoucherExpiryChatListener implements Listener {

    private final Main plugin;
    private final UxService uxService;

    public VoucherExpiryChatListener(Main plugin) {
        this.plugin = plugin;
        this.uxService = new UxService(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!plugin.getPendingVoucherExpiry().containsKey(uuid)) return;

        e.setCancelled(true);

        String message = e.getMessage().trim();

        // ===== CANCELAR =====
        if (message.equals("cancelar") || message.equals("0")) {
            plugin.getPendingVoucherExpiry().remove(uuid);

            player.sendMessage("§c❌ Definição de validade cancelada.");

            Bukkit.getScheduler().runTask(plugin, () -> {
                new AdminVoucherExpirySelectionGUI(plugin, player).open();
                uxService.playSoundFromConfig(player, "cancel");
            });
            return;
        }

        // ===== VALIDAR NUMERO =====
        try {
            int days = Integer.parseInt(message);

            // ❌ Cancelar manualmente
            if (days == 0) {
                plugin.getPendingVoucherExpiry().remove(uuid);
                player.sendMessage("§7Operação cancelada.");
                uxService.playSoundFromConfig(player, "cancel");
                return;
            }

            // ❌ Fora do intervalo
            if (days != -1 && (days < 1 || days > 3650)) {
                player.sendMessage("§cValor inválido. Use entre 1 e 3650 dias.");
                uxService.playSoundFromConfig(player, "error");
                return;
            }

            // ✅ Salva validade
            AdminVoucherExpirySelectionGUI.selectExpiry(player, days);
            plugin.getPendingVoucherExpiry().remove(uuid);

            // ✅ Abre confirmação (ADMIN)
            Bukkit.getScheduler().runTask(plugin, () ->
                    new VoucherConfirmationGUI(
                            plugin,
                            player,
                            AdminVoucherKeySelectionGUI.getSelectedKey(player),
                            days,
                            VoucherConfirmationGUI.Source.ADMIN
                    ).open()
            );

            uxService.playSoundFromConfig(player, "open_menu");

        } catch (NumberFormatException ex) {
            player.sendMessage("§cDigite um número válido, §f-1 §cpara infinito ou §f0 §cpara cancelar.");
            uxService.playSoundFromConfig(player, "error");
        }

    }

}
