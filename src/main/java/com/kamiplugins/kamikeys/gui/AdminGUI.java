package com.kamiplugins.kamikeys.gui;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminGUI {

    private final Main plugin;
    private final Player player;
    private String currentTab = "venda"; // venda, interna, exclusiva

    public AdminGUI(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(null, 54, ColorUtils.translate("&8&lKamiKeys Admin"));

        // Abas superiores
        createTabButton(inv, 0, "venda", "&bVenda");
        createTabButton(inv, 1, "interna", "&aEventos");
        createTabButton(inv, 2, "exclusiva", "&6Exclusivas");

        // Carrega keys da aba atual
        loadKeys(inv);

        // Botão de fechar
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ColorUtils.translate("&c&lFechar"));
        close.setItemMeta(closeMeta);
        inv.setItem(53, close);

        player.openInventory(inv);
    }

    private void createTabButton(Inventory inv, int slot, String tabId, String name) {
        Material mat = "venda".equals(tabId) ? Material.PAPER :
                "interna".equals(tabId) ? Material.MAP :
                        Material.NAME_TAG;

        if (currentTab.equals(tabId)) {
            mat = Material.EMERALD_BLOCK; // destaca aba ativa
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtils.translate(name));
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void loadKeys(Inventory inv) {
        FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
        if (!keysConfig.contains("keys")) return;

        int slot = 9;
        for (String key : keysConfig.getConfigurationSection("keys").getKeys(false)) {
            String origem = keysConfig.getString("keys." + key + ".origem", "");
            if (!origem.equals(currentTab)) continue;

            String tipo = keysConfig.getString("keys." + key + ".tipo", "desconhecido");
            String tipoFormatado = tipo.substring(0, 1).toUpperCase() + (tipo.length() > 1 ? tipo.substring(1).toLowerCase() : "");
            String color = plugin.getConfig().getString("Types." + tipo + ".PrefixColor", "&8");

            ItemStack keyItem = new ItemStack(Material.PAPER);
            ItemMeta meta = keyItem.getItemMeta();
            meta.setDisplayName(ColorUtils.translate(color + "[" + tipoFormatado + "] &f" + key));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.translate("&7Origem: &f" + origem));
            if ("exclusiva".equals(origem)) {
                String dono = keysConfig.getString("keys." + key + ".exclusivo_para.nome", "???");
                lore.add(ColorUtils.translate("&7Dono: &f" + dono));
            }
            lore.add("");
            lore.add(ColorUtils.translate("&eClique para copiar a key"));
            lore.add(ColorUtils.translate("&cShift+Clique para apagar"));
            meta.setLore(lore);

            keyItem.setItemMeta(meta);
            if (slot < 53) {
                inv.setItem(slot++, keyItem);
            }
        }
    }

    // Método para lidar com cliques (será chamado do Listener)
    public void handleClick(int slot, boolean isShift) {
        if (slot >= 9 && slot <= 52) {
            // Lógica de copiar/apagar será implementada no Listener
        } else if (slot == 0) {
            currentTab = "venda";
            open();
        } else if (slot == 1) {
            currentTab = "interna";
            open();
        } else if (slot == 2) {
            currentTab = "exclusiva";
            open();
        }
    }
}