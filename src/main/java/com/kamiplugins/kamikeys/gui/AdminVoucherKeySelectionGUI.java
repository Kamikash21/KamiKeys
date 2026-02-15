package com.kamiplugins.kamikeys.gui;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.models.enums.KeyOrigin;
import com.kamiplugins.kamikeys.models.enums.KeyState;
import com.kamiplugins.kamikeys.services.KeyService;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AdminVoucherKeySelectionGUI {
    private final Main plugin;
    private final Player player;
    private final KeyService keyService;
    private static final Map<UUID, Key> selectedKeys = new ConcurrentHashMap<>();
    private static final  int ITEMS_PER_PAGE = 45;
    private int currentPage = 0;
    private List<Key> keys;

    public AdminVoucherKeySelectionGUI(Main plugin, Player player, KeyService keyService) {
        this.plugin = plugin;
        this.player = player;
        this.keyService = keyService;
        reloadKeys();
    }

    public void open() {
        reloadKeys();

        int totalPages = getTotalPages();
        if (currentPage > totalPages - 1) {
            currentPage = Math.max(0, totalPages - 1);
        }

        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                "Selecionar Key - Página " + (currentPage + 1)
        );

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, keys.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Key key = keys.get(i);

            ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName("§b" + key.getCode());

            String typeKey = key.getTypeKey();
            String rewardTitle = ColorUtils.translate(
                    plugin.getConfig().getString("Types." + typeKey + ".Title", typeKey)
            );

            String status = switch (key.getState()) {
                case ATIVA -> "§a🟢 Ativa";
                default -> "§7● Desconhecido";
            };

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7✦ Recompensa: " + rewardTitle);
            lore.add("§7✦ Status: " + status);
            lore.add("");
            lore.add("§7Gerada em: §f" + key.getCreatedAt());
            lore.add(ColorUtils.translate("&7Gerada por: &d" + (key.getGeneratedBy() != null ? key.getGeneratedBy() : "Sistema")));
            lore.add("");
            lore.add("§eClique para selecionar esta key");

            meta.setLore(lore);
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
        }

        setupNavigationButtons(inventory);
        player.openInventory(inventory);
    }

    private void setupNavigationButtons(Inventory inventory) {
        boolean canGoPrev = currentPage > 0;
        boolean canGoNext = currentPage < (getTotalPages() - 1);

        inventory.setItem(45, createNavItem(Material.ARROW, "§7⬅ Voltar"));
        inventory.setItem(53, createNavItem(Material.BARRIER, "§c✖ Fechar"));

        inventory.setItem(48, createNavItem(
                canGoPrev ? Material.LIME_DYE : Material.GRAY_DYE,
                canGoPrev ? "§eAnterior" : "§7Anterior"
        ));

        inventory.setItem(49, createNavItem(
                Material.BOOK,
                "§7Página " + (currentPage + 1) + "/" + getTotalPages()
        ));

        inventory.setItem(50, createNavItem(
                canGoNext ? Material.LIME_DYE : Material.GRAY_DYE,
                canGoNext ? "§ePróxima" : "§7Próxima"
        ));
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            open();
        }
    }

    public void nextPage() {
        if (currentPage < getTotalPages() - 1) {
            currentPage++;
            open();
        }
    }

    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) keys.size() / ITEMS_PER_PAGE));
    }


    public static void selectKey(Player player, Key key) {
        selectedKeys.put(player.getUniqueId(), key);
    }

    public static Key getSelectedKey(Player player) {
        return selectedKeys.get(player.getUniqueId());
    }

    public static void clearSelection(Player player) {
        selectedKeys.remove(player.getUniqueId());
    }

    private void reloadKeys() {
        this.keys = keyService.getAllKeys().stream()

                // ✅ SOMENTE KEYS INTERNAS
                .filter(key -> key.getOrigin() == KeyOrigin.INTERNA)

                // ✅ SOMENTE KEYS ATIVAS
                .filter(key -> key.getState() == KeyState.ATIVA)

                .collect(Collectors.toList());
    }


    private ItemStack createNavItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.translate(displayName));
            item.setItemMeta(meta);
        }
        return item;
    }



}