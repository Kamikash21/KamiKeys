package com.kamiplugins.kamikeys.listeners;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.gui.*;
import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.models.enums.KeyOrigin;
import com.kamiplugins.kamikeys.services.KeyService;
import com.kamiplugins.kamikeys.services.ValidationService;
import com.kamiplugins.kamikeys.services.VoucherService;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import com.kamiplugins.kamikeys.utils.UxService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AdminSubmenuListener implements Listener {
    private final Main plugin;
    private final KeyService keyService;
    private final VoucherService voucherService;
    private final ValidationService validationService;
    private final UxService uxService;

    // Mapas para armazenar dados temporários
    private final Map<UUID, String> confirmationKeys = new HashMap<>();
    private final Map<UUID, String> voucherSelectionKeys = new HashMap<>();

    public AdminSubmenuListener(Main plugin, KeyService keyService, VoucherService voucherService, ValidationService validationService) {
        this.plugin = plugin;
        this.keyService = keyService;
        this.voucherService = voucherService;
        this.validationService = validationService;
        this.uxService = new UxService(plugin);

        // Registrar o listener no Bukkit
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdminSubmenuClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getClickedInventory();
        if (inv == null) return;

        String title = e.getView().getTitle();

        // Processar apenas menus de admin
        if (title.contains("Menu Admin - KamiKeys")) {
            handleAdminMainMenu(e, player, title);
        }
        else if (title.contains("Keys de Venda")) {
            handleKeysByTypeMenu(e, player, KeyOrigin.VENDA, title);
        }
        else if (title.contains("Keys Internas")) {
            handleKeysByTypeMenu(e, player, KeyOrigin.INTERNA, title);
        }
        else if (title.contains("Keys Exclusivas")) {
            handleKeysByTypeMenu(e, player, KeyOrigin.PLAYER, title);
        }
        else if (title.contains("Todas as Keys")) {
            handleKeysByTypeMenu(e, player,null, title);
        }
        // Processar menus de confirmação
        else if (title.contains("Confirmar Exclusão")) {
            handleDeletionConfirmation(e, player);
        }
        else if(title.contains("Selecionar Key ")) {
            handleVoucherKeySelection(e, player);
        }
        else if (title.contains("Selecionar Validade do Voucher")) {
           handleVoucherExpirySelection(e, player);
        }
    }

    private void handleAdminMainMenu(InventoryClickEvent e, Player player, String title) {
        e.setCancelled(true);

        int slot = e.getSlot();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String itemName = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : null;
        if (itemName == null) return;

        // Processar cliques nos itens do menu principal
        switch (slot) {
            case 11: // Keys de Venda
                AdminKeysByTypeGUI vendaGUI = new AdminKeysByTypeGUI(plugin, player, keyService, KeyOrigin.VENDA, "Keys de Venda");
                plugin.getAdminKeysGUIs().put(player.getUniqueId(), vendaGUI);
                vendaGUI.open();
                uxService.playSoundFromConfig(player, "open_menu");
                break;
            case 13: // Keys Internas
                AdminKeysByTypeGUI internaGUI = new AdminKeysByTypeGUI(plugin, player, keyService, KeyOrigin.INTERNA, "Keys Internas");
                plugin.getAdminKeysGUIs().put(player.getUniqueId(), internaGUI);
                internaGUI.open();
                uxService.playSoundFromConfig(player, "open_menu");
                break;
            case 15: // Keys Exclusivas
                AdminKeysByTypeGUI exclusivaGUI = new AdminKeysByTypeGUI(plugin, player, keyService, KeyOrigin.PLAYER, "Keys Exclusivas");
                plugin.getAdminKeysGUIs().put(player.getUniqueId(), exclusivaGUI);
                exclusivaGUI.open();
                uxService.playSoundFromConfig(player, "open_menu");
                break;
            case 21: // Todas as Keys
                AdminKeysByTypeGUI todasGUI = new AdminKeysByTypeGUI(plugin, player, keyService, null, "Todas as Keys");
                plugin.getAdminKeysGUIs().put(player.getUniqueId(), todasGUI);
                todasGUI.open();
                uxService.playSoundFromConfig(player, "open_menu");
                break;
            case 23: { // Voucher
                AdminVoucherKeySelectionGUI gui =
                        new AdminVoucherKeySelectionGUI(plugin, player, keyService);

                plugin.getAdminVoucherGUIs().put(player.getUniqueId(), gui);
                gui.open();

                uxService.playSoundFromConfig(player, "open_menu");
                break;
            }
            case 40: // Fechar
                player.closeInventory();
                uxService.playSoundFromConfig(player,"close_menu");
        }
    }

    private void handleKeysByTypeMenu(InventoryClickEvent e, Player player, KeyOrigin origin, String title) {
        e.setCancelled(true);

        int slot = e.getSlot();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String itemName = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : "";
        if (itemName == null) return;

        // Verificar botões de navegação (slots 45-53)
        if (slot >= 45 && slot <= 53) {
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (itemName.contains("Anterior")) {

                // Se estiver desativado (GRAY_DYE), não faz nada
                if (clicked.getType() == Material.GRAY_DYE) {
                    uxService.playSoundFromConfig(player, "error");
                    return;
                }

                uxService.playSoundFromConfig(player, "confirm_success");

                if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                    AdminKeysByTypeGUI gui = (AdminKeysByTypeGUI) plugin.getAdminKeysGUIs().get(player.getUniqueId());
                    gui.previousPage();
                }

            } else if (itemName.contains("Próxima")) {

                // Se estiver desativado (GRAY_DYE), não faz nada
                if (clicked.getType() == Material.GRAY_DYE) {
                    uxService.playSoundFromConfig(player, "error");
                    return;
                }

                uxService.playSoundFromConfig(player, "confirm_success");

                if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                    AdminKeysByTypeGUI gui = (AdminKeysByTypeGUI) plugin.getAdminKeysGUIs().get(player.getUniqueId());
                    gui.nextPage();
                }

            } else if (itemName.contains("Fechar")) {

                uxService.playSoundFromConfig(player, "close_menu");
                player.closeInventory();

            } else if (itemName.contains("Voltar")) {

                uxService.playSoundFromConfig(player, "voltar");
                new AdminMainMenu(plugin, player, keyService, validationService).open(player);

            }

            return;
        }


        // Processar cliques em keys (slots 0-44)
        if (slot >= 0 && slot <= 44) {
            String cleanName = ChatColor.stripColor(itemName);

            // Verificar se é uma key válida (não é botão de navegação)
            if (!cleanName.contains("Anterior") &&
                    !cleanName.contains("Próxima") &&
                    !cleanName.contains("Fechar") &&
                    !cleanName.contains("Voltar") &&
                    !cleanName.contains("Página")) {

                String keyCode = cleanName; // O nome do item é o código da key

                if (e.isLeftClick()) {
                    // Som de clique
                    uxService.playSoundFromConfig(player, "copy_key");
                    // Mensagem clicável para copiar (SUGGEST_COMMAND)
                    player.sendMessage(ColorUtils.translate(" "));
                    player.sendMessage(ColorUtils.translate("&a✅ Key selecionada com sucesso!"));

                    net.md_5.bungee.api.chat.TextComponent line =
                            new net.md_5.bungee.api.chat.TextComponent(ColorUtils.translate("&7📋 Clique aqui para copiar: &b" + keyCode));

                    // Hover
                    line.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                            net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                            new net.md_5.bungee.api.chat.BaseComponent[]{
                                    new net.md_5.bungee.api.chat.TextComponent(ColorUtils.translate("&eClique para copiar a key"))
                            }
                    ));

                    // Click -> sugestão no chat
                    line.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                            net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND,
                            keyCode
                    ));

                    player.spigot().sendMessage(line);
                    player.sendMessage(ColorUtils.translate(" "));

                    // Fechar menu depois (mantém seu comportamento atual)
                    player.closeInventory();

                } else if (e.isRightClick()) {
                    // Som de aviso
                    uxService.playSoundFromConfig(player, "open_menu");
                    // Abrir confirmação de exclusão
                    openDeletionConfirmationGUI(player, keyCode);
                }

            }
        }
    }


    private void handleDeletionConfirmation(InventoryClickEvent e, Player player) {
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String itemName = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : "";
        if (itemName == null) return;

        String keyCode = confirmationKeys.get(player.getUniqueId());
        if (keyCode == null) return;

        if (itemName.contains("🗑 Excluir")) {
            keyService.deleteKey(keyCode);

            player.sendMessage(ColorUtils.translate(" "));
            player.sendMessage(ColorUtils.translate("&c❌ Key &b" + keyCode + " &capagada com sucesso!"));
            player.sendMessage(ColorUtils.translate(" "));

            // ✅ Title + Subtitle
            player.sendTitle(
                    ColorUtils.translate("&c&lKEY APAGADA!"),
                    ColorUtils.translate("&7Key: &b" + keyCode),
                    10, 70, 10
            );

            // Som de confirmação
            uxService.playSoundFromConfig(player, "key_deleted");

            player.closeInventory();

            // Atualizar GUI
            if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                AdminKeysByTypeGUI gui = (AdminKeysByTypeGUI) plugin.getAdminKeysGUIs().get(player.getUniqueId());
                gui.refresh();
            }
        }
        else if (itemName.contains("Cancelar")) {
            player.closeInventory();
            if (plugin.getAdminKeysGUIs().containsKey(player.getUniqueId())) {
                AdminKeysByTypeGUI gui = (AdminKeysByTypeGUI) plugin.getAdminKeysGUIs().get(player.getUniqueId());
                gui.open();
            }
            uxService.playSoundFromConfig(player, "cancel");
        }

        confirmationKeys.remove(player.getUniqueId());
    }

    private void openDeletionConfirmationGUI(Player player, String keyCode) {
        Inventory confirmInv = Bukkit.createInventory(null, 27, "Confirmar Exclusão");

        ItemStack confirmItem = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ColorUtils.translate("&c🗑 Excluir Key"));

        java.util.List<String> confirmLore = new java.util.ArrayList<>();
        confirmLore.add("");
        confirmLore.add(ColorUtils.translate("&7Clique para confirmar a exclusão:"));
        confirmLore.add(ColorUtils.translate("&b" + keyCode));
        confirmLore.add("");
        confirmLore.add(ColorUtils.translate("&4⚠ Esta ação não pode ser desfeita!"));
        confirmMeta.setLore(confirmLore);

        confirmItem.setItemMeta(confirmMeta);
        confirmInv.setItem(11, confirmItem);

        ItemStack cancelItem = new ItemStack(Material.GRAY_CONCRETE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ColorUtils.translate("&7Cancelar"));

        java.util.List<String> cancelLore = new java.util.ArrayList<>();
        cancelLore.add(ColorUtils.translate("&7Clique para cancelar"));
        cancelMeta.setLore(cancelLore);

        cancelItem.setItemMeta(cancelMeta);
        confirmInv.setItem(15, cancelItem);

        confirmationKeys.put(player.getUniqueId(), keyCode);
        player.openInventory(confirmInv);
    }

    private void handleVoucherKeySelection(InventoryClickEvent e, Player player) {
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (!clicked.hasItemMeta()) return;

        String displayName = clicked.getItemMeta().getDisplayName();
        if (displayName == null) return;

        // ===== FECHAR =====
        if (clicked.getType() == Material.BARRIER) {
            AdminVoucherKeySelectionGUI.clearSelection(player);
            player.closeInventory();
            uxService.playSoundFromConfig(player, "close_menu");
            return;
        }

        // ===== VOLTAR =====
        if (clicked.getType() == Material.ARROW) {
            AdminVoucherKeySelectionGUI.clearSelection(player);
            plugin.getAdminVoucherGUIs().remove(player.getUniqueId());

            new AdminMainMenu(plugin, player, keyService, validationService).open(player);
            uxService.playSoundFromConfig(player, "voltar");
            return;
        }

        // ===== PAGINAÇÃO (PADRÃO ADMIN KEYS) =====
        if (displayName.contains("Anterior")) {

            if (clicked.getType() == Material.GRAY_DYE) {
                uxService.playSoundFromConfig(player, "error");
                return;
            }

            uxService.playSoundFromConfig(player, "confirm_success");

            if (plugin.getAdminVoucherGUIs().containsKey(player.getUniqueId())) {
                AdminVoucherKeySelectionGUI gui =
                        plugin.getAdminVoucherGUIs().get(player.getUniqueId());
                gui.previousPage();
            }
            return;
        }

        if (displayName.contains("Próxima")) {

            if (clicked.getType() == Material.GRAY_DYE) {
                uxService.playSoundFromConfig(player, "error");
                return;
            }

            uxService.playSoundFromConfig(player, "confirm_success");

            if (plugin.getAdminVoucherGUIs().containsKey(player.getUniqueId())) {
                AdminVoucherKeySelectionGUI gui =
                        plugin.getAdminVoucherGUIs().get(player.getUniqueId());
                gui.nextPage();
            }
            return;
        }



        // ===== SELEÇÃO DE KEY =====
        if (!displayName.startsWith("§b")) return;

        String keyCode = ChatColor.stripColor(displayName);
        Optional<Key> keyOpt = keyService.findByCode(keyCode);

        if (keyOpt.isPresent()) {
            AdminVoucherKeySelectionGUI.selectKey(player, keyOpt.get());
            uxService.playSoundFromConfig(player, "open_menu");
            new AdminVoucherExpirySelectionGUI(plugin, player).open();
        }
    }


    private void handleVoucherExpirySelection(InventoryClickEvent e, Player player) {
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // ===== FECHAR =====
        if (clicked.getType() == Material.BARRIER) {
            AdminVoucherKeySelectionGUI.clearSelection(player);
            player.closeInventory();
            uxService.playSoundFromConfig(player, "close_menu");
            return;
        }

        // ===== VOLTAR =====
        if (clicked.getType() == Material.ARROW) {
            AdminVoucherKeySelectionGUI.clearSelection(player);
            plugin.getAdminVoucherGUIs().remove(player.getUniqueId());

            new AdminMainMenu(plugin, player, keyService, validationService).open(player);
            uxService.playSoundFromConfig(player, "voltar");
            return;
        }

        // ===== VALOR MANUAL =====
        if (clicked.getType() == Material.ANVIL) {
            player.closeInventory();
            player.sendMessage(" ");
            player.sendMessage("§aDigite no chat a validade do voucher em dias.");
            player.sendMessage("§7Use §f-1 §7para validade infinita.");
            player.sendMessage("§cDigite §f0 §cou §fcancelar §cpara cancelar.");
            player.sendMessage(" ");

            plugin.getPendingVoucherExpiry().put(player.getUniqueId(), true);
            uxService.playSoundFromConfig(player, "confirm_success");
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        Integer expiryDays = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "voucher_expiry_days"),
                PersistentDataType.INTEGER
        );

        if (expiryDays == null) {
            player.sendMessage("§cEste item não possui validade vinculada.");
            uxService.playSoundFromConfig(player, "error");
            return;
        }

        // Proteção
        if (expiryDays != -1 && (expiryDays < 1 || expiryDays > 3650)) {
            player.sendMessage("§cValor inválido. Use entre 1 e 3650 dias.");
            uxService.playSoundFromConfig(player, "error");
            return;
        }

        AdminVoucherExpirySelectionGUI.selectExpiry(player, expiryDays);

        Key selectedKey = AdminVoucherKeySelectionGUI.getSelectedKey(player);
        int days = AdminVoucherExpirySelectionGUI.getSelectedExpiry(player);

        new VoucherConfirmationGUI(
                plugin,
                player,
                selectedKey,
                days,
                VoucherConfirmationGUI.Source.ADMIN
        ).open();

        uxService.playSoundFromConfig(player, "open_menu");

    }

}