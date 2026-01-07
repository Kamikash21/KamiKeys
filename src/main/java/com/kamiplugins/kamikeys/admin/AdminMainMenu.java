package com.kamiplugins.kamikeys.admin;

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

public class AdminMainMenu extends AdminBaseGUI {

    public AdminMainMenu(Main plugin, Player admin) {
        super(plugin, admin);
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(null, 27, "🔑 Menu Admin - KamiKeys");

        // Background personalizável
        String backgroundMaterial = plugin.getConfig().getString("Items.BackgroundPane", "RED_STAINED_GLASS_PANE");
        Material backgroundMat = Material.getMaterial(backgroundMaterial);
        if (backgroundMat == null) backgroundMat = Material.RED_STAINED_GLASS_PANE;

        ItemStack background = new ItemStack(backgroundMat);
        ItemMeta bgMeta = background.getItemMeta();
        bgMeta.setDisplayName(" ");
        background.setItemMeta(bgMeta);

        // Preenche todo o inventário com background
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, background);
        }

        // Item: Keys de Venda (configurável)
        String vendaMaterial = plugin.getConfig().getString("Items.AdminMenuVenda", "GOLD_INGOT");
        Material vendaMat = Material.getMaterial(vendaMaterial);
        if (vendaMat == null) vendaMat = Material.GOLD_INGOT;

        ItemStack vendaItem = new ItemStack(vendaMat);
        ItemMeta vendaMeta = vendaItem.getItemMeta();
        vendaMeta.setDisplayName(ColorUtils.translate("&e💰 Keys de Venda"));

// Lore premium para Keys de Venda
        int vendaCount = countKeysByOrigin("venda");
        String lastVenda = getLastKeyByOrigin("venda");

        List<String> vendaLore = new ArrayList<>();
        vendaLore.add(ColorUtils.translate("&7Geradas para vendas externas"));
        vendaLore.add(ColorUtils.translate(""));
        vendaLore.add(ColorUtils.translate("&f➤ &aDisponíveis: &f" + vendaCount));
        if (!lastVenda.isEmpty()) {
            vendaLore.add(ColorUtils.translate("&f➤ &7Última: &f" + lastVenda));
        }
        vendaLore.add(ColorUtils.translate(""));
        vendaLore.add(ColorUtils.translate("&e✨ Clique para gerenciar"));
        vendaMeta.setLore(vendaLore);
        vendaItem.setItemMeta(vendaMeta);
        inv.setItem(10, vendaItem);

// Item: Keys Internas (configurável)
        String internaMaterial = plugin.getConfig().getString("Items.AdminMenuInterna", "DIAMOND");
        Material internaMat = Material.getMaterial(internaMaterial);
        if (internaMat == null) internaMat = Material.DIAMOND;

        ItemStack internaItem = new ItemStack(internaMat);
        ItemMeta internaMeta = internaItem.getItemMeta();
        internaMeta.setDisplayName(ColorUtils.translate("&b🎁 Keys Internas"));

// Lore premium para Keys Internas
        int internaCount = countKeysByOrigin("interna");
        String lastInterna = getLastKeyByOrigin("interna");

        List<String> internaLore = new ArrayList<>();
        internaLore.add(ColorUtils.translate("&7Usadas em eventos e recompensas"));
        internaLore.add(ColorUtils.translate(""));
        internaLore.add(ColorUtils.translate("&f➤ &aDisponíveis: &f" + internaCount));
        if (!lastInterna.isEmpty()) {
            internaLore.add(ColorUtils.translate("&f➤ &7Última: &f" + lastInterna));
        }
        internaLore.add(ColorUtils.translate(""));
        internaLore.add(ColorUtils.translate("&b🎉 Clique para gerenciar"));
        internaMeta.setLore(internaLore);
        internaItem.setItemMeta(internaMeta);
        inv.setItem(12, internaItem);

// Item: Keys Exclusivas (configurável)
        String exclusivaMaterial = plugin.getConfig().getString("Items.AdminMenuExclusiva", "PLAYER_HEAD");
        Material exclusivaMat = Material.getMaterial(exclusivaMaterial);
        if (exclusivaMat == null) exclusivaMat = Material.PLAYER_HEAD;

        ItemStack exclusivaItem = new ItemStack(exclusivaMat);
        ItemMeta exclusivaMeta = exclusivaItem.getItemMeta();
        exclusivaMeta.setDisplayName(ColorUtils.translate("&c👤 Keys Exclusivas"));

// Lore premium para Keys Exclusivas
        int exclusivaCount = countKeysByOrigin("exclusiva");
        String lastExclusiva = getLastKeyByOrigin("exclusiva");

        List<String> exclusivaLore = new ArrayList<>();
        exclusivaLore.add(ColorUtils.translate("&7Vinculadas a jogadores específicos"));
        exclusivaLore.add(ColorUtils.translate(""));
        exclusivaLore.add(ColorUtils.translate("&f➤ &aDisponíveis: &f" + exclusivaCount));
        if (!lastExclusiva.isEmpty()) {
            exclusivaLore.add(ColorUtils.translate("&f➤ &7Última: &f" + lastExclusiva));
        }
        exclusivaLore.add(ColorUtils.translate(""));
        exclusivaLore.add(ColorUtils.translate("&c🔒 Clique para gerenciar"));
        exclusivaMeta.setLore(exclusivaLore);
        exclusivaItem.setItemMeta(exclusivaMeta);
        inv.setItem(14, exclusivaItem);

// Item: Todas as Keys (configurável)
        String todasMaterial = plugin.getConfig().getString("Items.AdminMenuTodas", "ENCHANTED_BOOK");
        Material todasMat = Material.getMaterial(todasMaterial);
        if (todasMat == null) todasMat = Material.ENCHANTED_BOOK;

        ItemStack todasItem = new ItemStack(todasMat);
        ItemMeta todasMeta = todasItem.getItemMeta();
        todasMeta.setDisplayName(ColorUtils.translate("&a📚 Todas as Keys"));

// Lore premium para Todas as Keys
        int totalCount = countKeysByOrigin("todas");
        List<String> todasLore = new ArrayList<>();
        todasLore.add(ColorUtils.translate("&7Visão completa de todas as keys"));
        todasLore.add(ColorUtils.translate(""));
        todasLore.add(ColorUtils.translate("&f➤ &aTotal: &f" + totalCount));
        todasLore.add(ColorUtils.translate("&f➤ &7Origens: venda, interna, exclusiva"));
        todasLore.add(ColorUtils.translate(""));
        todasLore.add(ColorUtils.translate("&a📊 Clique para visualizar tudo"));
        todasMeta.setLore(todasLore);
        todasItem.setItemMeta(todasMeta);
        inv.setItem(16, todasItem);

        // Sons premium
        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
            String sound = plugin.getConfig().getString("Sounds.Admin.MenuOpen", "block.shulker_box.open");
            admin.playSound(admin.getLocation(), sound, 0.7f, 1.2f);
        }

        admin.openInventory(inv);
    }

    @Override
    public void refresh() {
        this.open();
    }

    @Override
    public int getCurrentPage() {
        return 0;
    }

    @Override
    public void nextPage() {
        // Não usado
    }

    @Override
    public void previousPage() {
        // Não usado
    }

    private int countKeysByOrigin(String origin) {
        FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
        if (!keysConfig.contains("keys")) return 0;

        if ("todas".equals(origin)) {
            return keysConfig.getConfigurationSection("keys").getKeys(false).size();
        }

        int count = 0;
        for (String key : keysConfig.getConfigurationSection("keys").getKeys(false)) {
            String keyOrigin = keysConfig.getString("keys." + key + ".origem", "");
            if (origin.equals(keyOrigin)) count++;
        }
        return count;
    }

    private String getLastKeyByOrigin(String origin) {
        FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
        if (!keysConfig.contains("keys")) return "";

        String lastKey = "";
        String lastDate = "";

        for (String key : keysConfig.getConfigurationSection("keys").getKeys(false)) {
            String keyOrigin = keysConfig.getString("keys." + key + ".origem", "");
            String keyDate = keysConfig.getString("keys." + key + ".data_geracao", "1970-01-01 00:00:00");

            if (origin.equals(keyOrigin)) {
                if (keyDate.compareTo(lastDate) > 0) {
                    lastDate = keyDate;
                    lastKey = key;
                }
            }
        }

        return lastKey.isEmpty() ? "" : lastKey.substring(0, Math.min(12, lastKey.length())) + "...";
    }
}