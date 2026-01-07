package com.kamiplugins.kamikeys.admin;

import com.kamiplugins.kamikeys.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AdminMainMenuListener implements Listener {

    private final Main plugin;

    public AdminMainMenuListener(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin); // ← Essa linha é crucial
    }

    @EventHandler
    public void onAdminMainMenuClick(InventoryClickEvent e) {
        // Log para verificar se o evento está sendo chamado
        plugin.getLogger().info("DEBUG: Evento de clique chamado - Titulo: " + e.getView().getTitle());

        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getClickedInventory();

        // Cancela o evento IMEDIATAMENTE
        e.setCancelled(true);

        if (inv == null) {
            plugin.getLogger().info("DEBUG: Inventory é null");
            return;
        }

        if (!e.getView().getTitle().contains("Menu Admin")) {
            plugin.getLogger().info("DEBUG: Titulo não corresponde");
            return;
        }

        plugin.getLogger().info("DEBUG: Menu principal detectado - cancelando evento");

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            plugin.getLogger().info("DEBUG: Item clicado é null ou AIR");
            return;
        }

        String itemName = clicked.getItemMeta().getDisplayName();
        if (itemName == null) {
            plugin.getLogger().info("DEBUG: Nome do item é null");
            return;
        }

        if (itemName.contains("Keys de Venda")) {
            AdminKeysByTypeGUI gui = new AdminKeysByTypeGUI(plugin, player, "venda", "💰 Keys de Venda");
            plugin.getAdminKeysGUIs().put(player.getUniqueId(), gui);
            gui.open();
        } else if (itemName.contains("Keys Internas")) {
            AdminKeysByTypeGUI gui = new AdminKeysByTypeGUI(plugin, player, "interna", "🎁 Keys Internas");
            plugin.getAdminKeysGUIs().put(player.getUniqueId(), gui);
            gui.open();
        } else if (itemName.contains("Keys Exclusivas")) {
            AdminKeysByTypeGUI gui = new AdminKeysByTypeGUI(plugin, player, "exclusiva", "👤 Keys Exclusivas");
            plugin.getAdminKeysGUIs().put(player.getUniqueId(), gui);
            gui.open();
        } else if (itemName.contains("Todas as Keys")) {
            AdminKeysByTypeGUI gui = new AdminKeysByTypeGUI(plugin, player, "todas", "📚 Todas as Keys");
            plugin.getAdminKeysGUIs().put(player.getUniqueId(), gui);
            gui.open();
        }
    }

    @EventHandler
    public void onAdminMenuClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();

        // SÓ processa se for o menu principal do admin
        if (!title.equals("🔑 Menu Admin - KamiKeys")) {
            return; // ← Adicione esta linha crucial!
        }

        // Resto do código do menu principal...
    }
}