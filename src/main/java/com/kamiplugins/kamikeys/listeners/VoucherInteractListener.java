package com.kamiplugins.kamikeys.listeners;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.services.VoucherService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class VoucherInteractListener implements Listener {

    private final VoucherService voucherService;

    public VoucherInteractListener(Main plugin, VoucherService voucherService) {
        this.voucherService = voucherService;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onVoucherUse(PlayerInteractEvent event) {

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) return;

        // 🔑 É um voucher?
        if (!voucherService.isVoucherItem(item)) return;

        // Bloqueia uso padrão (ex: plantar sunflower)
        event.setCancelled(true);

        // 🔥 Ativação real
        voucherService.activateVoucher(player, item);
    }
}
