package com.kamiplugins.kamikeys.listeners;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.gui.PlayerVoucherKeySelectionGUI;
import com.kamiplugins.kamikeys.gui.VoucherConfirmationGUI;
import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.services.KeyService;
import com.kamiplugins.kamikeys.utils.UxService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

public class PlayerVoucherKeySelectionListener implements Listener {

    private final Main plugin;
    private final KeyService keyService;
    private final UxService uxService;

    public PlayerVoucherKeySelectionListener(Main plugin, KeyService keyService) {
        this.plugin = plugin;
        this.keyService = keyService;
        this.uxService = plugin.getConfigManager().getUxService();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) return;

        // ✅ Identifica por HOLDER
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof PlayerVoucherKeySelectionGUI.Holder)) return;

        // ✅ Cancela tudo (top e bottom)
        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;

        // ✅ Só processa click no TOP
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        UUID uuid = player.getUniqueId();

        PlayerVoucherKeySelectionGUI gui = plugin.getPlayerVoucherGUIs().get(uuid);
        if (gui == null) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String name = meta.getDisplayName();
        Material type = item.getType();

        // FECHAR
        if (type == Material.BARRIER && name.contains("Fechar")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                plugin.getPlayerVoucherGUIs().remove(uuid);
                uxService.playSoundFromConfig(player, "close_menu");
            });
            return;
        }

        // VOLTAR
        if (type == Material.ARROW && name.contains("Voltar")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                plugin.getPlayerVoucherGUIs().remove(uuid);
                Bukkit.dispatchCommand(player, "keys");
                uxService.playSoundFromConfig(player, "voltar");
            });
            return;
        }

        if (type == Material.BOOK){
            // Apenas indicador de página
            uxService.playSoundFromConfig(player, "reload");
            return;
        }

        // PÁGINA ANTERIOR
        if (name.contains("Anterior")) {
            if (type == Material.GRAY_DYE) {
                uxService.playSoundFromConfig(player, "error");
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                gui.previousPage();
                gui.open();
                uxService.playSoundFromConfig(player, "confirm_pending");
            });
            return;
        }

        // PRÓXIMA PÁGINA
        if (name.contains("Próxima")) {
            if (type == Material.GRAY_DYE) {
                uxService.playSoundFromConfig(player, "error");
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                gui.nextPage();
                gui.open();
                uxService.playSoundFromConfig(player, "confirm_pending");
            });
            return;
        }

        // SELEÇÃO
        String keyCode = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "voucher_select_key"),
                PersistentDataType.STRING
        );

        if (keyCode == null) return;

        Optional<Key> keyOpt = keyService.findByCode(keyCode);
        if (keyOpt.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                uxService.playSoundFromConfig(player, "error");
                player.closeInventory();
                plugin.getPlayerVoucherGUIs().remove(uuid);
            });
            return;
        }

        Key key = keyOpt.get();
        PlayerVoucherKeySelectionGUI.select(player, key);

        int days = plugin.getConfig().getInt("Voucher.DefaultValidityDays", 30);
        // aceita -1 (infinito) e aceita 0 (expira imediatamente)
        if (days < -1) days = 30;

        plugin.getPlayerVoucherGUIs().remove(uuid);

        int finalDays = days;
        Bukkit.getScheduler().runTask(plugin, () -> {
            new VoucherConfirmationGUI(
                    plugin,
                    player,
                    key,
                    finalDays,
                    VoucherConfirmationGUI.Source.PLAYER
            ).open();

            uxService.playSoundFromConfig(player, "open_menu");
        });
    }
}
