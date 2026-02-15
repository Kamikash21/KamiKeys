package com.kamiplugins.kamikeys.listeners;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.services.VoucherService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class VoucherLoreRefreshListener implements Listener {

    private final Main plugin;
    private final VoucherService voucherService;

    public VoucherLoreRefreshListener(Main plugin, VoucherService voucherService) {
        this.plugin = plugin;
        this.voucherService = voucherService;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ✅ Atualiza quando o player clica/move no inventário
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Inventory inv = event.getInventory();

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (ItemStack item : inv.getContents()) {
                plugin.getVoucherService().refreshVoucherLore(item);
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (current != null) {
                plugin.getVoucherService().refreshVoucherLore(current);
            }
            if (cursor != null) {
                plugin.getVoucherService().refreshVoucherLore(cursor);
            }
        });
    }


    // ✅ Atualiza quando o player entra (corrige itens antigos no inventário)
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (ItemStack item : player.getInventory().getContents()) {
                plugin.getVoucherService().refreshVoucherLore(item);
            }
        }, 20L);
    }

}
