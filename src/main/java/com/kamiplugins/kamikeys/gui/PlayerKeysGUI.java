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

public class PlayerKeysGUI {

    private final Main plugin;
    private final Player player;
    private final List<String> playerKeys;
    private int currentPage = 0;
    private static final int KEYS_PER_PAGE = 45;

    public PlayerKeysGUI(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.playerKeys = getPlayerExclusiveKeys();
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

    private List<String> getPlayerExclusiveKeys() {
        List<String> keys = new ArrayList<>();
        FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
        if (keysConfig.contains("keys")) {
            for (String key : keysConfig.getConfigurationSection("keys").getKeys(false)) {
                String dono = keysConfig.getString("keys." + key + ".exclusivo_para.nome");
                if (dono != null && dono.equalsIgnoreCase(player.getName())) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    public void open() {
        openPage(currentPage);
        // Som ao abrir o menu (shulker box)
        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
            player.playSound(player.getLocation(), "block.shulker_box.open", 0.7f, 1.2f);
        }
    }

    private void openPage(int page) {
        int totalPages = (int) Math.ceil((double) playerKeys.size() / KEYS_PER_PAGE);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        if (totalPages == 0) page = 0;

        this.currentPage = page;

        Inventory inv = Bukkit.createInventory(null, 54, "🔑 Minhas Keys - Página " + (page + 1) + "/" + Math.max(1, totalPages));

        if (!playerKeys.isEmpty()) {
            int startIndex = page * KEYS_PER_PAGE;
            int endIndex = Math.min(startIndex + KEYS_PER_PAGE, playerKeys.size());
            int slot = 0;

            for (int i = startIndex; i < endIndex; i++) {
                String key = playerKeys.get(i);
                String tipo = plugin.getConfigManager().getKeysConfig().getString("keys." + key + ".tipo", "desconhecido");

                // Lê recompensa do config.yml (agora com descrição real)
                String recompensa = getRewardDescription(tipo);

                ItemStack item = new ItemStack(Material.valueOf(
                        plugin.getConfig().getString("Items.PlayerKeyItem", "TRIPWIRE_HOOK")
                ));
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ColorUtils.translate("&b" + key)); // ← COR AZUL

                List<String> lore = new ArrayList<>();
                lore.add(""); // ← ESPAÇO EM BRANCO ANTES DA RECOMPENSA
                lore.add(ColorUtils.translate("&7🎁 Recompensa: &e" + recompensa));
                lore.add("");
                lore.add(ColorUtils.translate("&a✅ Clique esquerdo: &2Ativar"));
                lore.add(ColorUtils.translate("&c❌ Clique direito: &4Excluir"));

                meta.setLore(lore);
                item.setItemMeta(meta);

                inv.setItem(slot, item);
                slot++;
            }
        }

        // Botões de navegação (sempre aparecem)
// Anterior
        if (page > 0) {
            ItemStack prevItem = new ItemStack(Material.valueOf(
                    plugin.getConfig().getString("Items.PreviousButton", "ARROW")
            ));
            ItemMeta prevMeta = prevItem.getItemMeta();
            prevMeta.setDisplayName(ColorUtils.translate("&e◀ Anterior"));
            prevItem.setItemMeta(prevMeta);
            inv.setItem(48, prevItem);
        } else {
            // Item desativado para "anterior"
            ItemStack disabledPrevItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta disabledPrevMeta = disabledPrevItem.getItemMeta();
            disabledPrevMeta.setDisplayName(ColorUtils.translate("&7◀ Anterior"));
            disabledPrevItem.setItemMeta(disabledPrevMeta);
            inv.setItem(48, disabledPrevItem);
        }

// Página atual
        ItemStack pageItem = new ItemStack(Material.BOOK);
        ItemMeta pageMeta = pageItem.getItemMeta();
        pageMeta.setDisplayName(ColorUtils.translate("&7Página &f" + (page + 1) + " &7de &f" + Math.max(1, totalPages)));
        pageItem.setItemMeta(pageMeta);
        inv.setItem(49, pageItem);

// Próxima
        if (page < totalPages - 1) {
            ItemStack nextItem = new ItemStack(Material.valueOf(
                    plugin.getConfig().getString("Items.NextButton", "ARROW")
            ));
            ItemMeta nextMeta = nextItem.getItemMeta();
            nextMeta.setDisplayName(ColorUtils.translate("&ePróxima ▶"));
            nextItem.setItemMeta(nextMeta);
            inv.setItem(50, nextItem);
        } else {
            // Item desativado para "próxima"
            ItemStack disabledNextItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta disabledNextMeta = disabledNextItem.getItemMeta();
            disabledNextMeta.setDisplayName(ColorUtils.translate("&7Próxima ▶"));
            disabledNextItem.setItemMeta(disabledNextMeta);
            inv.setItem(50, disabledNextItem);
        }

// Fechar
        ItemStack closeItem = new ItemStack(Material.valueOf(
                plugin.getConfig().getString("Items.CloseButton", "BARRIER")
        ));
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ColorUtils.translate("&c❌ Fechar"));
        closeItem.setItemMeta(closeMeta);
        inv.setItem(53, closeItem);

        player.openInventory(inv);
    }

    private String getRewardDescription(String tipo) {
        // Lê o Title do config.yml para mostrar como recompensa
        String title = plugin.getConfig().getString("Types." + tipo + ".Title");
        if (title != null) {
            return ColorUtils.translate(title); // Retorna o título traduzido
        }

        // Se não tiver Title, tenta montar a partir dos comandos (fallback)
        List<String> comandos = plugin.getConfig().getStringList("Types." + tipo + ".Commands");
        if (comandos.isEmpty()) return "Recompensa desconhecida";

        for (String cmd : comandos) {
            if (cmd.contains("playerpoints")) {
                String[] parts = cmd.split(" ");
                if (parts.length >= 4 && parts[2].equals("give")) {
                    String quantidade = parts[3];
                    return quantidade + " coins";
                }
            }
        }

        return "Recompensa personalizada";
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void nextPage() {
        if ((currentPage + 1) * KEYS_PER_PAGE < playerKeys.size()) {
            openPage(currentPage + 1);
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            openPage(currentPage - 1);
        }
    }

    public void refresh() {
        // Atualiza a lista de keys e abre a página atual
        this.playerKeys.clear();
        this.playerKeys.addAll(getPlayerExclusiveKeys());
        openPage(currentPage);
    }
}