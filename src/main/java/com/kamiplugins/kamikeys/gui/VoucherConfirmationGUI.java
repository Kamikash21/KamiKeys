package com.kamiplugins.kamikeys.gui;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.models.Voucher;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import com.kamiplugins.kamikeys.utils.UxService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class VoucherConfirmationGUI implements Listener {

    public enum Source {
        PLAYER,
        ADMIN
    }

    private final Main plugin;
    private final Player player;
    private final Key key;
    private final int days;
    private final Source source;
    private final UxService uxService;
    private boolean actionLocked = false;

    public VoucherConfirmationGUI(
            Main plugin,
            Player player,
            Key key,
            int days,
            Source source
    ) {
        this.plugin = plugin;
        this.player = player;
        this.key = key;
        this.days = days;
        this.source = source;
        this.uxService = new UxService(plugin);

        // ✅ registra o listener UMA vez por instância
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* =========================
       HOLDER
       ========================= */
    public static class Holder implements InventoryHolder {
        private final Source source;

        public Holder(Source source) {
            this.source = source;
        }

        public Source getSource() {
            return source;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    /* =========================
       OPEN
       ========================= */
    public void open() {

        Inventory inv = Bukkit.createInventory(
                new Holder(source),
                27,
                "Confirmar Voucher"
        );

        // ===== RESUMO =====
        ItemStack summary = new ItemStack(Material.BOOK);
        ItemMeta sm = summary.getItemMeta();
        sm.setDisplayName("§eResumo do Voucher");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Key: §b" + key.getCode());
        lore.add("§7Recompensa: §f" + plugin.getConfig()
                .getString("Types." + key.getTypeKey() + ".Recompensa", "Recompensa"));

        lore.add(days == -1
                ? "§7Validade: §a∞ Infinita"
                : "§7Validade: §a" + days + " dias");

        lore.add("");
        lore.add("§c⚠ Esta ação não pode ser desfeita");

        sm.setLore(lore);
        summary.setItemMeta(sm);
        inv.setItem(13, summary);

        // ===== CONFIRMAR =====
        inv.setItem(11, createButton(
                Material.LIME_CONCRETE,
                "§a✅ Confirmar",
                List.of("§7Criar o voucher")
        ));

        // ===== CANCELAR =====
        inv.setItem(15, createButton(
                Material.RED_CONCRETE,
                "§c❌ Cancelar",
                List.of("§7Voltar sem criar")
        ));

        // ===== DECORAÇÃO =====
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta gm = glass.getItemMeta();
                gm.setDisplayName("");
                glass.setItemMeta(gm);
                inv.setItem(i, glass);
            }
        }

        player.openInventory(inv);
        uxService.playSoundFromConfig(player, "open_menu");
    }

    private ItemStack createButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /* =========================
       LISTENER (CLICK)
       ========================= */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!p.getUniqueId().equals(player.getUniqueId())) return;

        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof Holder)) return;

        // ✅ bloqueia tudo
        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        // ✅ roda no próximo tick pra evitar travar o menu
        Bukkit.getScheduler().runTask(plugin, () -> handleClick(clicked));
    }

    /* =========================
       HANDLE CLICK
       ========================= */
    public void handleClick(ItemStack clicked) {

        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (!clicked.hasItemMeta()) return;
        if (actionLocked) return;

        String name = clicked.getItemMeta().getDisplayName();
        if (name == null) return;

        // ===== CANCELAR =====
        if (name.contains("❌")) {
            actionLocked = true;
            uxService.playSoundFromConfig(player, "cancel");
            player.closeInventory();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (source == Source.PLAYER) {
                    Bukkit.dispatchCommand(player, "keys");
                } else {
                    Bukkit.dispatchCommand(player, "kamikeys list");
                }
            });
            return;
        }

        // ===== CONFIRMAR =====
        if (name.contains("Confirmar")) {
            actionLocked = true;

            if (player.getInventory().firstEmpty() == -1) {
                player.closeInventory();
                player.sendMessage(ColorUtils.translate(" "));
                player.sendMessage(ColorUtils.translate("&c❌ Seu inventário está cheio."));
                player.sendMessage(ColorUtils.translate("&7Libere espaço para criar voucher."));
                player.sendMessage(ColorUtils.translate(" "));
                player.sendTitle(ColorUtils.translate("&cInventário Cheio!"),ColorUtils.translate("&7Libere espaço"), 10, 70, 20);
                uxService.playSoundFromConfig(player, "error");
                return;
            }

            try {
                Voucher voucher = plugin.getVoucherService().createVoucherFromKey(
                        key,
                        player.getUniqueId().toString(),
                        player.getName(),
                        days
                );

                player.getInventory().addItem(
                        plugin.getVoucherService().createVoucherItemStack(voucher, key)
                );

                player.sendMessage(ColorUtils.translate("&a✅ Voucher criado com sucesso!"));
                uxService.playSoundFromConfig(player, "success");

            } catch (Exception e) {
                player.sendMessage(ColorUtils.translate("&c❌ Erro ao criar voucher."));
                uxService.playSoundFromConfig(player, "error");
            } finally {
                player.closeInventory();
            }
        }
    }
}
