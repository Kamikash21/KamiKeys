package com.kamiplugins.kamikeys.gui;

import com.kamiplugins.kamikeys.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AdminVoucherExpirySelectionGUI {

    private final Main plugin;
    private final Player player;

    private static final Map<UUID, Integer> selectedExpiry = new ConcurrentHashMap<>();

    // 🔥 Opções premium + infinito
    private static final int[] EXPIRY_OPTIONS = {1, 7, 30, 90, 365, -1};

    public AdminVoucherExpirySelectionGUI(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        Inventory inventory = Bukkit.createInventory(null, 54, "Selecionar Validade do Voucher");

        // ===== OPÇÕES PRINCIPAIS =====
        inventory.setItem(10, createDaysItem(1));
        inventory.setItem(12, createDaysItem(7));
        inventory.setItem(14, createDaysItem(30));
        inventory.setItem(16, createDaysItem(90));
        inventory.setItem(29, createDaysItem(365));

        // ===== OPÇÕES ESPECIAIS =====
        inventory.setItem(31, createInfiniteItem());
        inventory.setItem(33, createCustomItem());

        // ===== VOLTAR =====
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§7⬅ Voltar");
        backItem.setItemMeta(backMeta);
        inventory.setItem(45, backItem);

        // ===== FECHAR =====
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§c✖ Fechar");
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(53, closeItem);

        player.openInventory(inventory);
    }



    private ItemStack createDaysItem(int days) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§a⏳ " + days + " dias");
        meta.setLore(List.of(
                "§8──────────────",
                "§7Validade do voucher:",
                "§f" + days + " dias",
                "",
                "§7Após esse período",
                "§7o voucher expirará.",
                "",
                "§e▶ Clique para selecionar",
                "§8──────────────"
        ));

        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "voucher_expiry_days"),
                PersistentDataType.INTEGER,
                days
        );

        item.setItemMeta(meta);
        return item;

    }


    private ItemStack createInfiniteItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§d∞ Validade infinita");
        meta.setLore(List.of(
                "§8──────────────",
                "§7Validade do voucher:",
                "§dInfinita",
                "",
                "§7Este voucher não",
                "§7possui data de expiração.",
                "",
                "§e▶ Clique para selecionar",
                "§8──────────────"
        ));

        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "voucher_expiry_days"),
                PersistentDataType.INTEGER,
                -1
        );

        item.setItemMeta(meta);
        return item;
    }


    private ItemStack createCustomItem() {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§b✏ Definir valor manual");
        meta.setLore(List.of(
                "§8──────────────",
                "§7Defina manualmente",
                "§7a validade do voucher.",
                "",
                "§7Digite no chat:",
                "§f• 30 §8→ 30 dias",
                "§f• -1 §8→ infinito",
                "",
                "§b▶ Clique para digitar",
                "§8──────────────"
        ));

        item.setItemMeta(meta);
        return item;
    }


    public static void selectExpiry(Player player, int days) {
        selectedExpiry.put(player.getUniqueId(), days);
    }

    public static int getSelectedExpiry(Player player) {
        return selectedExpiry.getOrDefault(player.getUniqueId(), 7);
    }

    public static void clearSelection(Player player) {
        selectedExpiry.remove(player.getUniqueId());
    }
}
