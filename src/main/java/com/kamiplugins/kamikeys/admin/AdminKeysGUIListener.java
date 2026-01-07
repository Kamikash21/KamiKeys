package com.kamiplugins.kamikeys.admin;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.commands.AtivarCommand;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminKeysGUIListener implements Listener {

    private final Main plugin;
    private final Map<UUID, String> adminConfirmationKeys = new HashMap<>();
    private final Map<UUID, String> adminActivationKeys = new HashMap<>();

    public AdminKeysGUIListener(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onAdminInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getClickedInventory();
        if (inv == null) return;

        if (!e.getView().getTitle().contains("Todas as Keys")) {
            return;
        }

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String itemName = clicked.getItemMeta().getDisplayName();
        if (itemName == null) return;

        FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();

        // Botões de navegação (verificação sem formatação)
        String cleanName = ChatColor.stripColor(itemName);

        if (cleanName.contains("Anterior")) {
            if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                plugin.getAdminKeysGUIs().get(player.getUniqueId()).previousPage();
                // Som de navegação (shulker box)
                if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
                    player.playSound(player.getLocation(), "block.shulker_box.open", 0.5f, 1.0f);
                }
            }
            return;
        }
        if (cleanName.contains("Próxima")) {
            if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                plugin.getAdminKeysGUIs().get(player.getUniqueId()).nextPage();
                // Som de navegação (shulker box)
                if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
                    player.playSound(player.getLocation(), "block.shulker_box.open", 0.5f, 1.0f);
                }
            }
            return;
        }
        if (cleanName.contains("Fechar")) {
            player.closeInventory();
            // Som de fechar (shulker box)
            if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
                player.playSound(player.getLocation(), "block.shulker_box.close", 0.5f, 0.8f);
            }
            return;
        }
        if (cleanName.contains("Página")) {
            return; // Não faz nada ao clicar no item de página
        }

        // Verifica se é uma key
        if (!cleanName.contains("<<") && !cleanName.contains(">>") && !cleanName.contains("Fechar") && !cleanName.contains("Página")) {
            String key = cleanName; // ← Usa o nome limpo

            if (e.isLeftClick()) {
                // Verifica se a key ainda existe antes de abrir confirmação
                if (!keysConfig.contains("keys." + key)) {
                    player.sendMessage(ColorUtils.translate("&c❌ Key &b" + key + " &cjá foi usada ou removida!"));
                    // Atualiza a GUI para remover o item
                    if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                        plugin.getAdminKeysGUIs().get(player.getUniqueId()).refresh();
                    }
                    return;
                }

                // Abrir GUI de confirmação para ativação (admin)
                openAdminActivationConfirmationGUI(player, key);
            } else if (e.isRightClick()) {
                // Verifica se a key ainda existe antes de abrir confirmação
                if (!keysConfig.contains("keys." + key)) {
                    player.sendMessage(ColorUtils.translate("&c❌ Key &b" + key + " &cjá foi usada ou removida!"));
                    // Atualiza a GUI para remover o item
                    if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                        plugin.getAdminKeysGUIs().get(player.getUniqueId()).refresh();
                    }
                    return;
                }

                // Abrir GUI de confirmação para exclusão (admin)
                openAdminConfirmationGUI(player, key);
            }
        }
    }

    private void openAdminActivationConfirmationGUI(Player player, String key) {
        Inventory confirmInv = Bukkit.createInventory(null, 27, "Confirmar Ativação (Admin)");

        // Item de confirmação
        ItemStack confirmItem = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ColorUtils.translate("&a✅ Ativar Key"));

        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ColorUtils.translate(""));
        confirmLore.add(ColorUtils.translate("&7Clique para confirmar a ativação:"));
        confirmLore.add(ColorUtils.translate("&b" + key));
        confirmLore.add("");
        confirmLore.add(ColorUtils.translate("&e⚠️ Esta ação não pode ser desfeita!"));
        confirmMeta.setLore(confirmLore);

        confirmItem.setItemMeta(confirmMeta);
        confirmInv.setItem(11, confirmItem);

        // Item de cancelamento
        ItemStack cancelItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ColorUtils.translate("&c❌ Cancelar"));

        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ColorUtils.translate("&7Clique para cancelar"));
        cancelMeta.setLore(cancelLore);

        cancelItem.setItemMeta(cancelMeta);
        confirmInv.setItem(15, cancelItem);

        // Armazena a key para confirmação de ativação (admin)
        adminActivationKeys.put(player.getUniqueId(), key);

        player.openInventory(confirmInv);

        // Som de abertura
        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
            player.playSound(player.getLocation(), "block.barrel.open", 0.7f, 1.2f);
        }
    }

    private void openAdminConfirmationGUI(Player player, String key) {
        Inventory confirmInv = Bukkit.createInventory(null, 27, "Confirmar Exclusão (Admin)");

        // Item de confirmação
        ItemStack confirmItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ColorUtils.translate("&c🗑️ Excluir Key"));

        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ColorUtils.translate(""));
        confirmLore.add(ColorUtils.translate("&7Clique para confirmar a exclusão:"));
        confirmLore.add(ColorUtils.translate("&b" + key));
        confirmLore.add("");
        confirmLore.add(ColorUtils.translate("&c⚠️ Esta ação não pode ser desfeita!"));
        confirmMeta.setLore(confirmLore);

        confirmItem.setItemMeta(confirmMeta);
        confirmInv.setItem(11, confirmItem);

        // Item de cancelamento
        ItemStack cancelItem = new ItemStack(Material.GRAY_CONCRETE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ColorUtils.translate("&7Cancelar"));

        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ColorUtils.translate("&7Clique para cancelar"));
        cancelMeta.setLore(cancelLore);

        cancelItem.setItemMeta(cancelMeta);
        confirmInv.setItem(15, cancelItem);

        // Armazena a key para confirmação de exclusão (admin)
        adminConfirmationKeys.put(player.getUniqueId(), key);

        player.openInventory(confirmInv);

        // Som de abertura
        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
            player.playSound(player.getLocation(), "block.ender_chest.open", 0.7f, 1.2f);
        }
    }

    @EventHandler
    public void onAdminActivationConfirmationClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getClickedInventory();
        if (inv == null) return;

        if (!e.getView().getTitle().contains("Confirmar Ativação (Admin)")) {
            return;
        }

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String itemName = clicked.getItemMeta().getDisplayName();
        if (itemName == null) return;

        String key = adminActivationKeys.get(player.getUniqueId());
        if (key == null) return;

        if (itemName.contains("✅ Ativar")) {
            // Ativar a key (admin)
            AtivarCommand ativarCmd = new AtivarCommand(plugin);
            ativarCmd.activateKey(player, key, player.getName()); // Pode usar o nome do admin como "ativador"

            // Fecha a GUI de confirmação
            player.closeInventory();

            // Aguarda um tick para garantir que a key foi removida
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Atualiza a GUI principal
                if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                    plugin.getAdminKeysGUIs().get(player.getUniqueId()).refresh();
                }
            }, 1L); // 1 tick de delay

            // Som de sucesso
            if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
                player.playSound(player.getLocation(), "entity.player.levelup", 0.8f, 1.0f);
            }
        } else if (itemName.contains("❌ Cancelar")) {
            // Fecha a GUI e volta para a GUI de keys
            player.closeInventory();
            if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                plugin.getAdminKeysGUIs().get(player.getUniqueId()).open();
            }

            // Som de cancelamento
            if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
                player.playSound(player.getLocation(), "block.barrel.close", 0.5f, 0.8f);
            }
        }

        adminActivationKeys.remove(player.getUniqueId());
    }

    @EventHandler
    public void onAdminConfirmationClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getClickedInventory();
        if (inv == null) return;

        if (!e.getView().getTitle().contains("Confirmar Exclusão (Admin)")) {
            return;
        }

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String itemName = clicked.getItemMeta().getDisplayName();
        if (itemName == null) return;

        String key = adminConfirmationKeys.get(player.getUniqueId());
        if (key == null) return;

        if (itemName.contains("🗑️ Excluir")) {
            // Excluir a key (admin)
            FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
            if (keysConfig.contains("keys." + key)) {
                keysConfig.set("keys." + key, null);
                plugin.getConfigManager().saveKeys();

                // Feedback visual
                player.sendMessage(ColorUtils.translate("&c❌ Key &b" + key + " &capagada com sucesso!"));
                player.playSound(player.getLocation(), "entity.experience_orb.pickup", 0.7f, 1.2f);
            } else {
                player.sendMessage(ColorUtils.translate("&c❌ Key já foi removida!"));
            }

            // Fecha a GUI de confirmação
            player.closeInventory();

            // Atualiza a GUI principal (sem delay)
            if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                plugin.getAdminKeysGUIs().get(player.getUniqueId()).refresh();
            }

            // Som de exclusão
            if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
                player.playSound(player.getLocation(), "block.ender_chest.close", 0.6f, 0.8f);
            }
        }
        else if (itemName.contains("Cancelar")) {
            // Fecha a GUI e volta para a GUI de keys
            player.closeInventory();
            if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                plugin.getAdminKeysGUIs().get(player.getUniqueId()).open();
            }

            // Som de cancelamento
            if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
                player.playSound(player.getLocation(), "block.ender_chest.close", 0.5f, 0.8f);
            }
        }

        adminConfirmationKeys.remove(player.getUniqueId());
    }
}