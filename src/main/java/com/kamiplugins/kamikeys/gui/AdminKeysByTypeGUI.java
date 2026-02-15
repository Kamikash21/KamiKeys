package com.kamiplugins.kamikeys.gui;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.models.enums.KeyOrigin;
import com.kamiplugins.kamikeys.models.enums.KeyState;
import com.kamiplugins.kamikeys.services.KeyService;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class AdminKeysByTypeGUI {
    private final Main plugin;
    private final KeyService keyService;
    private final Player player;
    private final KeyOrigin origin;
    private final String title;
    private int currentPage = 0;
    private final int itemsPerPage = 45;
    private List<Key> keys;
    private final boolean showOrigin;

    public AdminKeysByTypeGUI(Main plugin, Player player, KeyService keyService, KeyOrigin origin, String title) {
        this.plugin = plugin;
        this.keyService = keyService;
        this.player = player;
        this.origin = origin;
        this.title = title;
        this.showOrigin = (origin == null);

        reloadKeys();

    }

    public void open() {
        reloadKeys();
        int totalPages = getTotalPages();
        if (currentPage > totalPages - 1) {
            currentPage = Math.max(0, totalPages - 1);
        }

        Inventory inventory = Bukkit.createInventory(null, 54, title + " - Página " + (currentPage + 1));

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, keys.size());

        for (int i = startIndex; i < endIndex; i++) {
            Key key = keys.get(i);
            ItemStack keyItem = keyService.createKeyItemStack(key, player, showOrigin);
            int slot = i - startIndex;
            inventory.setItem(slot, keyItem);
        }

        setupNavigationButtons(inventory);

        player.openInventory(inventory);
    }

    private void setupNavigationButtons(Inventory inventory) {

        boolean canGoPrev = currentPage > 0;
        boolean canGoNext = currentPage < (getTotalPages() - 1);

        // ===== Botão Anterior =====
        Material prevMaterial = canGoPrev ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack prevItem = new ItemStack(prevMaterial);
        ItemMeta prevMeta = prevItem.getItemMeta();
        prevMeta.setDisplayName(ColorUtils.translate(canGoPrev ? "&eAnterior" : "&7Anterior"));
        prevItem.setItemMeta(prevMeta);
        inventory.setItem(48, prevItem);

        // ===== Página =====
        ItemStack pageItem = new ItemStack(Material.BOOK);
        ItemMeta pageMeta = pageItem.getItemMeta();
        pageMeta.setDisplayName(ColorUtils.translate("&7Página " + (currentPage + 1) + "/" + getTotalPages()));
        pageItem.setItemMeta(pageMeta);
        inventory.setItem(49, pageItem);

        // ===== Botão Próxima =====
        Material nextMaterial = canGoNext ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack nextItem = new ItemStack(nextMaterial);
        ItemMeta nextMeta = nextItem.getItemMeta();
        nextMeta.setDisplayName(ColorUtils.translate(canGoNext ? "&ePróxima" : "&7Próxima"));
        nextItem.setItemMeta(nextMeta);
        inventory.setItem(50, nextItem);

        // ===== Fechar =====
        ItemStack closeItem = new ItemStack(Material.valueOf(plugin.getConfig().getString("Items.CloseButton", "BARRIER")));
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ColorUtils.translate("§c✖ Fechar"));
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(53, closeItem);

        // ===== Voltar =====
        ItemStack backItem = new ItemStack(Material.valueOf(plugin.getConfig().getString("Items.BackButton", "ARROW")));
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ColorUtils.translate("§7⬅ Voltar"));
        backItem.setItemMeta(backMeta);
        inventory.setItem(45, backItem);
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
        return Math.max(1, (int) Math.ceil((double) keys.size() / itemsPerPage));
    }

    public void refresh() {
        reloadKeys();

        open();
    }

    public Player getPlayer() {
        return player;
    }

    public KeyService getKeyService() {
        return keyService;
    }

    public KeyOrigin getOrigin() {
        return origin;
    }

    private void reloadKeys() {
        this.keys = keyService.getAllKeys().stream()

                // ✅ Regra global: só keys ATIVAS
                .filter(key ->
                        key.getState() == KeyState.ATIVA ||
                                key.getState() == KeyState.VENDA ||
                                key.getState() == KeyState.RESERVADA ||
                                key.getState() == KeyState.VENDIDA ||
                                key.getState() == KeyState.BLOQUEADA
                )


                // ✅ Regra de cada menu
                .filter(key -> {
                    // TODAS AS KEYS
                    if (origin == null) {
                        return true;
                    }

                    // VENDA: origin VENDA
                    if (origin == KeyOrigin.VENDA) {
                        return key.getOrigin() == KeyOrigin.VENDA
                                && key.getState() == KeyState.VENDA;
                    }


                    // INTERNA: origin INTERNA + SEM DONO
                    if (origin == KeyOrigin.INTERNA) {
                        return key.getOrigin() == KeyOrigin.INTERNA
                                && key.getState() == KeyState.ATIVA
                                && key.getExclusiveToName() == null;
                    }


                    // EXCLUSIVA: TEM DONO
                    if (origin == KeyOrigin.PLAYER) {
                        return key.getExclusiveToName() != null
                                && (key.getState() == KeyState.ATIVA || key.getState() == KeyState.VENDIDA);
                    }


                    return false;
                })

                // 🔥 ORDENAR: MAIS RECENTE PRIMEIRO
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }



}