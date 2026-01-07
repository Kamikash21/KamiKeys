package com.kamiplugins.kamikeys.admin;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.commands.AtivarCommand;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
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

// IMPORTS CORRETOS PARA COLEÇÕES
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminSubmenuListener implements Listener {

    private final Main plugin;
    private final Map<UUID, String> adminConfirmationKeys = new HashMap<>();
    private final Map<UUID, String> adminActivationKeys = new HashMap<>();

    public AdminSubmenuListener(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onAdminSubmenuClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getClickedInventory();
        if (inv == null) return;

        // Verifica se é um submenu do admin
        String title = e.getView().getTitle();
        if (!title.contains("Keys de Venda") &&
                !title.contains("Keys Internas") &&
                !title.contains("Keys Exclusivas") &&
                !title.contains("Todas as Keys")) {
            return;
        }

        // Cancela o evento (impede pegar itens)
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String itemName = clicked.getItemMeta().getDisplayName();
        if (itemName == null) return;

        // Verifica se é item de navegação
        String cleanName = ChatColor.stripColor(itemName);

        if (cleanName.contains("Anterior")) {
            if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                plugin.getAdminKeysGUIs().get(player.getUniqueId()).previousPage();
                playNavigationSound(player);
            }
            return;
        }
        if (cleanName.contains("Próxima")) {
            if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                plugin.getAdminKeysGUIs().get(player.getUniqueId()).nextPage();
                playNavigationSound(player);
            }
            return;
        }
        if (cleanName.contains("Fechar")) {
            player.closeInventory();
            playCloseSound(player);

            // Volta para o Menu Principal do Admin
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                AdminMainMenu mainMenu = new AdminMainMenu(plugin, player);
                plugin.getAdminKeysGUIs().put(player.getUniqueId(), mainMenu);
                mainMenu.open();
            }, 1L);

            return;
        }
        if (cleanName.contains("Página")) {
            return; // Não faz nada
        }

        // Verifica se é uma key (nome azul com formato de key)
        if (itemName != null && itemName.startsWith("§b")) {
            String key = ChatColor.stripColor(itemName);

            // Verifica se a key ainda existe
            FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
            if (!keysConfig.contains("keys." + key)) {
                player.sendMessage(ColorUtils.translate("&c❌ Key já foi usada ou removida!"));
                refreshGUI(player);
                return;
            }

            if (e.isLeftClick()) {
                // COPIA A KEY (não ativa)
                copyKeyToAdmin(player, key);
            } else if (e.isRightClick()) {
                // Exclusão com confirmação
                openDeletionConfirmation(player, key);
            }
        }
    }

    private void openDeletionConfirmation(Player player, String key) {
        Inventory confirmInv = Bukkit.createInventory(null, 27, "Confirmar Exclusão (Admin)");

        ItemStack confirmItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ColorUtils.translate("&c🗑 Excluir Key"));

        // 🔥 LORE COM COLORUTILS TRANSLATE EM CADA LINHA 🔥
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("");
        confirmLore.add(ColorUtils.translate("&7Clique para confirmar a exclusão:"));
        confirmLore.add(ColorUtils.translate("&b" + key));
        confirmLore.add("");
        confirmLore.add(ColorUtils.translate("&c⚠ Esta ação não pode ser desfeita!"));

        confirmMeta.setLore(confirmLore);
        confirmItem.setItemMeta(confirmMeta);
        confirmInv.setItem(11, confirmItem);

        ItemStack cancelItem = new ItemStack(Material.GRAY_CONCRETE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ColorUtils.translate("&7Cancelar"));

        // 🔥 LORE DO CANCELAMENTO TAMBÉM PRECISA 🔥
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ColorUtils.translate("&7Clique para cancelar"));
        cancelMeta.setLore(cancelLore);

        cancelItem.setItemMeta(cancelMeta);
        confirmInv.setItem(15, cancelItem);

        adminConfirmationKeys.put(player.getUniqueId(), key);
        player.openInventory(confirmInv);
        playOpenSound(player);
    }

    @EventHandler
    public void onConfirmationClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();

        // SÓ processa menus de confirmação de exclusão
        if (!title.equals("Confirmar Exclusão (Admin)")) {
            return;
        }

        // Cancela o evento (impede pegar itens)
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String itemName = clicked.getItemMeta().getDisplayName();
        if (itemName == null) {
            return;
        }

        // EXECUTA DIRETAMENTE - já sabemos que é o menu certo!
        handleDeletionConfirmation(player, itemName);
    }

    private void handleDeletionConfirmation(Player player, String itemName) {
        String key = adminConfirmationKeys.get(player.getUniqueId());
        if (key == null) {
            player.closeInventory();
            return;
        }

        player.closeInventory();

        // 🔥 REMOVER CORES ANTES DE COMPARAR 🔥
        String cleanName = ChatColor.stripColor(itemName);

        if (cleanName.contains("🗑 Excluir")) {
            FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
            if (keysConfig.contains("keys." + key)) {
                keysConfig.set("keys." + key, null);
                plugin.getConfigManager().saveKeys();
                player.sendMessage(ColorUtils.translate("&c❌ Key &b" + key + " &capagada com sucesso!"));
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                refreshGUI(player);
            }, 1L);

            playDeleteSound(player);
        }
        else if (cleanName.contains("Cancelar")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                reopenPreviousGUI(player);
            }, 1L);

            playCancelSound(player);
        }

        adminConfirmationKeys.remove(player.getUniqueId());
    }

    private void refreshGUI(Player player) {
        if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
            plugin.getAdminKeysGUIs().get(player.getUniqueId()).refresh();
        }
    }

    private void reopenPreviousGUI(Player player) {
        if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
            plugin.getAdminKeysGUIs().get(player.getUniqueId()).open();
        }
    }

    private void playNavigationSound(Player player) {
        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
            player.playSound(player.getLocation(), "block.shulker_box.open", 0.5f, 1.0f);
        }
    }

    private void playCloseSound(Player player) {
        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
            player.playSound(player.getLocation(), "block.shulker_box.close", 0.5f, 0.8f);
        }
    }

    private void playOpenSound(Player player) {
        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
            player.playSound(player.getLocation(), "block.chest.open", 0.7f, 1.2f);
        }
    }

    private void playDeleteSound(Player player) {
        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
            player.playSound(player.getLocation(), "block.glass.break", 0.6f, 0.8f);
        }
    }

    private void playCancelSound(Player player) {
        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
            player.playSound(player.getLocation(), "block.note_block.bass", 0.5f, 0.8f);
        }
    }

    private void copyKeyToAdmin(Player player, String key) {
        player.sendMessage("");

        // Mensagem principal
        TextComponent message = new TextComponent(ColorUtils.translate("&a📋 Key administrativa: "));

        // Parte clicável da key
        TextComponent keyComponent = new TextComponent(key);
        keyComponent.setColor(net.md_5.bungee.api.ChatColor.AQUA);
        keyComponent.setBold(false);

        // SUGERE COMANDO ao clicar (funciona no Java)
        String suggestCommand = "&b" + key;
        keyComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestCommand));

        // Hover: instrução clara
        keyComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(ColorUtils.translate("&aClique para copiar a key"))));

        player.closeInventory();
        message.addExtra(keyComponent);
        player.spigot().sendMessage(message);

        player.sendMessage("");
        player.sendMessage(ColorUtils.translate("&e💡 &fDica: &7Copie esta key somente para fins administrativos"));
        player.sendMessage(ColorUtils.translate("&c⚠ &7O manuseio inadequado de keys é &ccrítico&7,"));
        player.sendMessage(ColorUtils.translate("&7pode causar &cbugs, perda de itens &7e até &cpunições&f!"));

        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
            player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.5f);
        }

    }


}