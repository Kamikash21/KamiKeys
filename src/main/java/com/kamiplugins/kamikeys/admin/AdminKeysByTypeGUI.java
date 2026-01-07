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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class AdminKeysByTypeGUI extends AdminBaseGUI {

    private final String title;
    private final String filterType;
    private List<String> displayedKeys;
    private int currentPage = 0;
    private static final int KEYS_PER_PAGE = 45;

    public AdminKeysByTypeGUI(Main plugin, Player admin, String filterType, String title) {
        super(plugin, admin);
        this.filterType = filterType;
        this.title = title;
        this.displayedKeys = loadAndSortKeys();
    }

    private List<String> loadAndSortKeys() {
        List<Map.Entry<String, String>> keysWithDates = new ArrayList<>();
        FileConfiguration config = plugin.getConfigManager().getKeysConfig();

        if (!config.contains("keys")) return new ArrayList<>();

        for (String key : config.getConfigurationSection("keys").getKeys(false)) {
            String origem = config.getString("keys." + key + ".origem", "");

            if ("todas".equals(filterType) || origem.equals(filterType)) {
                String date = config.getString("keys." + key + ".data_geracao", "1970-01-01 00:00:00");
                keysWithDates.add(new AbstractMap.SimpleEntry<>(key, date));
            }
        }

        // Ordena por data (mais recentes primeiro)
        keysWithDates.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<String> sortedKeys = new ArrayList<>();
        for (Map.Entry<String, String> entry : keysWithDates) {
            sortedKeys.add(entry.getKey());
        }

        return sortedKeys;
    }

    @Override
    public void open() {
        openPage(currentPage);

        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
            String sound = plugin.getConfig().getString("Sounds.Admin.SubmenuOpen", "block.chest.open");
            admin.playSound(admin.getLocation(), sound, 0.7f, 1.2f);
        }
    }

    private void openPage(int page) {
        int totalPages = (int) Math.ceil((double) displayedKeys.size() / KEYS_PER_PAGE);
        if (page < 0) page = 0;
        if (totalPages > 0 && page >= totalPages) page = totalPages - 1;

        this.currentPage = page;

        Inventory inv = Bukkit.createInventory(null, 54, title + " - Página " + (page + 1) + "/" + Math.max(1, totalPages));

        // Background premium
        setupBackground(inv);

        // Preencher keys
        if (!displayedKeys.isEmpty()) {
            int start = page * KEYS_PER_PAGE;
            int end = Math.min(start + KEYS_PER_PAGE, displayedKeys.size());

            for (int i = start, slot = 0; i < end; i++, slot++) {
                String key = displayedKeys.get(i);
                inv.setItem(slot, createKeyItem(key));
            }
        }

        // Botões de navegação premium
        setupNavigationButtons(inv, page, totalPages);
        admin.openInventory(inv);
    }

    private void setupBackground(Inventory inv) {
        String backgroundMaterial = plugin.getConfig().getString("Items.BackgroundPane", "LIGHT_GRAY_STAINED_GLASS_PANE");
        Material backgroundMat = Material.getMaterial(backgroundMaterial);
        if (backgroundMat == null) backgroundMat = Material.LIGHT_GRAY_STAINED_GLASS_PANE;

        ItemStack background = new ItemStack(backgroundMat);
        ItemMeta bgMeta = background.getItemMeta();
        bgMeta.setDisplayName(" ");
        background.setItemMeta(bgMeta);

        // Preenche slots 0-44 (área de keys) + 45-53 (navegação)
        for (int i = 0; i < 54; i++) {
            // Não sobrescreve slots que terão conteúdo
            if (i >= 45 && i <= 53) continue;
            inv.setItem(i, background);
        }
    }

    private ItemStack createKeyItem(String key) {
        FileConfiguration config = plugin.getConfigManager().getKeysConfig();

        String tipo = config.getString("keys." + key + ".tipo", "desconhecido");
        String donoExclusivo = config.getString("keys." + key + ".exclusivo_para.nome", "Nenhum");
        String geradoPor = config.getString("keys." + key + ".gerador", "Sistema");
        long timestamp = config.getLong("keys." + key + ".gerado_em", 0);
        String dataGeracao = convertTimestampToDate(timestamp);
        String origem = config.getString("keys." + key + ".origem", "desconhecida");

        // Determina o "Dono" baseado na origem
        String donoLore;
        if ("exclusiva".equals(origem) && !"Nenhum".equals(donoExclusivo)) {
            donoLore = donoExclusivo;
        } else if ("venda".equals(origem)) {
            donoLore = "Loja";
        } else if ("interna".equals(origem)) {
            donoLore = "KamiMC";
        } else {
            donoLore = "Sistema";
        }

        // Item personalizável
        String keyMaterial = plugin.getConfig().getString("Items.AdminKeyItem", "TRIPWIRE_HOOK");
        Material keyMat = Material.getMaterial(keyMaterial);
        if (keyMat == null) keyMat = Material.TRIPWIRE_HOOK;

        ItemStack item = new ItemStack(keyMat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtils.translate("&b" + key));

        // Lore premium
        List<String> lore = new ArrayList<>();
        lore.add(""); // Espaço

        // Recompensa
        String recompensa = getRewardDescription(tipo);
        lore.add(ColorUtils.translate("&7🎁 Recompensa: &e" + recompensa));

        // Tipo com prefixo colorido
        String prefixColor = plugin.getConfig().getString("Types." + tipo + ".PrefixColor", "&8");
        String tipoFormatado = tipo.substring(0, 1).toUpperCase() + (tipo.length() > 1 ? tipo.substring(1).toLowerCase() : "");
        lore.add(ColorUtils.translate("&7🏷 Tipo: " + prefixColor + "[" + tipoFormatado + "]"));

        // Dono baseado na origem
        lore.add(ColorUtils.translate("&7👤 Dono: &f" + donoLore));

        // Origem
        lore.add(ColorUtils.translate("&7📦 Origem: &f" + origem));

        // Gerado por
        lore.add(ColorUtils.translate("&7🖨 Gerado por: &f" + geradoPor));

        // Data de geração
        lore.add(ColorUtils.translate("&7📅 Gerada em: &f" + dataGeracao));

        lore.add(""); // Espaço
        lore.add(ColorUtils.translate("&a📋 &fClique esquerdo: &2Copiar Key"));
        lore.add(ColorUtils.translate("&c❌ &fClique direito: &4Excluir"));

        meta.setLore(lore);
        item.setItemMeta(meta);



        return item;
    }

    private void setupNavigationButtons(Inventory inv, int page, int totalPages) {
        // Anterior
        if (page > 0) {
            inv.setItem(48, createNavigationItem(
                    plugin.getConfig().getString("Items.PreviousButton", "ARROW"),
                    "&e◀ Anterior"
            ));
        } else {
            inv.setItem(48, createDisabledNavigationItem("&8◀ Anterior"));
        }

        // Página atual
        inv.setItem(49, createNavigationItem(
                "PAPER",
                "&7Página &f" + (page + 1) + " &7de &f" + Math.max(1, totalPages)
        ));

        // Próxima
        if (page < totalPages - 1) {
            inv.setItem(50, createNavigationItem(
                    plugin.getConfig().getString("Items.NextButton", "ARROW"),
                    "&ePróxima ▶"
            ));
        } else {
            inv.setItem(50, createDisabledNavigationItem("&8Próxima ▶"));
        }

        // Fechar
        inv.setItem(53, createNavigationItem(
                plugin.getConfig().getString("Items.CloseButton", "BARRIER"),
                "&cFechar"
        ));
    }

    private ItemStack createNavigationItem(String materialName, String displayName) {
        Material mat = Material.getMaterial(materialName.toUpperCase());
        if (mat == null) mat = Material.ARROW;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtils.translate(displayName));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDisabledNavigationItem(String displayName) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtils.translate(displayName));
        item.setItemMeta(meta);
        return item;
    }

    private String getRewardDescription(String tipo) {
        String title = plugin.getConfig().getString("Types." + tipo + ".Title");
        return title != null ? ColorUtils.translate(title) : "Recompensa personalizada";
    }

    @Override
    public void refresh() {
        this.displayedKeys = loadAndSortKeys();
        open();
    }

    @Override
    public int getCurrentPage() {
        return currentPage;
    }

    @Override
    public void nextPage() {
        if ((currentPage + 1) * KEYS_PER_PAGE < displayedKeys.size()) {
            openPage(currentPage + 1);
        }
    }

    @Override
    public void previousPage() {
        if (currentPage > 0) {
            openPage(currentPage - 1);
        }
    }

    public String getKeyGenerator(String key) {
        return plugin.getConfigManager().getKeysConfig().getString("keys." + key + ".gerado_por", "Sistema");
    }

    public String getKeyGenerationDate(String key) {
        return plugin.getConfigManager().getKeysConfig().getString("keys." + key + ".data_geracao", "Desconhecida");
    }

    private String convertTimestampToDate(long timestamp) {
        if (timestamp == 0) return "Desconhecida";
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(timestamp * 1000L));
    }
}