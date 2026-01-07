package com.kamiplugins.kamikeys.listeners;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AdminGUIListener implements Listener {

    private final Main plugin;

    public AdminGUIListener(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getClickedInventory();
        if (inv == null) return;

        if (!e.getView().getTitle().contains("KamiKeys Admin")) { // ← CORREÇÃO AQUI
            return;
        }

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String itemName = clicked.getItemMeta().getDisplayName();
        if (itemName == null) return;

        // Verifica se é uma key (tem formato de key no nome)
        if (itemName.contains("] &f") && !itemName.contains("Venda") && !itemName.contains("Eventos") && !itemName.contains("Exclusivas") && !itemName.contains("Fechar")) {
            String key = extractKeyFromName(itemName);

            if (e.isLeftClick()) {
                // Copiar key
                player.sendMessage(ColorUtils.translate("&aKey copiada para o chat: &f" + key));
                player.performCommand("say " + key); // workaround para "copiar"
            } else if (e.isRightClick()) {
                // Apagar key
                plugin.getConfigManager().getKeysConfig().set("keys." + key, null);
                plugin.getConfigManager().saveKeys();
                player.sendMessage(ColorUtils.translate("&cKey &f" + key + " &capagada com sucesso!"));
                e.getInventory().remove(e.getCurrentItem());
            }
        }
    }

    private String extractKeyFromName(String displayName) {
        // Extrai a key do nome do item: "[Basica] ABC123" -> ABC123
        String[] parts = displayName.split(" &f");
        if (parts.length > 1) {
            return parts[1];
        }
        return null;
    }
}