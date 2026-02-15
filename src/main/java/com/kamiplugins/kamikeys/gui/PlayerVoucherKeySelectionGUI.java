package com.kamiplugins.kamikeys.gui;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerVoucherKeySelectionGUI {

    // ✅ HOLDER PRÓPRIO
    public static class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final ConcurrentHashMap<UUID, Key> selectedKey = new ConcurrentHashMap<>();

    private final Main plugin;
    private final Player player;
    private final List<Key> activeKeys;

    private int currentPage = 0;

    private static final int ITEMS_PER_PAGE = 45;

    public PlayerVoucherKeySelectionGUI(Main plugin, Player player, List<Key> activeKeys) {
        this.plugin = plugin;
        this.player = player;
        this.activeKeys = activeKeys;
    }

    public void open() {
        Inventory inventory = Bukkit.createInventory(new Holder(), 54, "Selecione a Key para Voucher");

        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, activeKeys.size());

        for (int i = start; i < end; i++) {
            Key key = activeKeys.get(i);
            inventory.setItem(i - start, createKeyItem(key));
        }

        setupNavigationButtons(inventory);
        player.openInventory(inventory);
    }

    private ItemStack createKeyItem(Key key) {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§b" + key.getCode());

        String reward = plugin.getConfig().getString(
                "Types." + key.getTypeKey() + ".Recompensa",
                "Recompensa"
        );

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7🎁 Recompensa: §e" + MessageUtils.applyColor(reward));
        lore.add("§a🟢 Status: Ativa");
        lore.add("");
        lore.add("§7Transforme esta key");
        lore.add("§7em um voucher físico.");
        lore.add("");
        lore.add("§eClique para selecionar");

        meta.setLore(lore);

        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "voucher_select_key"),
                PersistentDataType.STRING,
                key.getCode()
        );

        item.setItemMeta(meta);
        return item;
    }

    private void setupNavigationButtons(Inventory inventory) {

        boolean canGoPrev = currentPage > 0;
        boolean canGoNext = currentPage < (getTotalPages() - 1);

        // SLOT 45 — VOLTAR
        inventory.setItem(45, createNavItem(Material.ARROW, "§7⬅ Voltar"));

        // SLOT 48 — PÁGINA ANTERIOR
        inventory.setItem(48, createNavItem(
                canGoPrev ? Material.LIME_DYE : Material.GRAY_DYE,
                canGoPrev ? "§e◀ Página Anterior" : "§7◀ Página Anterior"
        ));

        // SLOT 49 — INDICADOR
        inventory.setItem(49, createNavItem(
                Material.BOOK,
                "§7Página §f" + (currentPage + 1) + " §7de §f" + getTotalPages()
        ));

        // SLOT 50 — PRÓXIMA PÁGINA
        inventory.setItem(50, createNavItem(
                canGoNext ? Material.LIME_DYE : Material.GRAY_DYE,
                canGoNext ? "§ePróxima Página ▶" : "§7Próxima Página ▶"
        ));

        // SLOT 53 — FECHAR
        inventory.setItem(53, createNavItem(Material.BARRIER, "§c✖ Fechar"));
    }

    private ItemStack createNavItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public void nextPage() {
        if (currentPage < getTotalPages() - 1) {
            currentPage++;
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
        }
    }

    private int getTotalPages() {
        return Math.max(1,
                (int) Math.ceil((double) activeKeys.size() / ITEMS_PER_PAGE)
        );
    }

    // ===== SELEÇÃO =====

    public static void select(Player player, Key key) {
        selectedKey.put(player.getUniqueId(), key);
    }

    public static Key getSelected(Player player) {
        return selectedKey.get(player.getUniqueId());
    }

    public static void clear(Player player) {
        selectedKey.remove(player.getUniqueId());
    }
}
