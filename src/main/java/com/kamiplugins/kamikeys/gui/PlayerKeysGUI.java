package com.kamiplugins.kamikeys.gui;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.interfaces.KeysMenuHolder;
import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.models.enums.KeyState;
import com.kamiplugins.kamikeys.services.KeyService;
import com.kamiplugins.kamikeys.services.ValidationService;
import com.kamiplugins.kamikeys.services.VoucherService;
import com.kamiplugins.kamikeys.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerKeysGUI {
    private final Main plugin;
    private final KeyService keyService;
    private final VoucherService voucherService;
    private final ValidationService validationService;
    private final Player player;
    private int currentPage = 0;
    private final int itemsPerPage = 45; // Slots 0-44
    private final List<Key> playerKeys;

    public PlayerKeysGUI(Main plugin, Player player, KeyService keyService, VoucherService voucherService, ValidationService validationService) {
        this.plugin = plugin;
        this.keyService = keyService;
        this.voucherService = voucherService;
        this.validationService = validationService;
        this.player = player;

        // Carregar keys exclusivas do jogador
        this.playerKeys = keyService.getAllKeys().stream()
                .filter(key -> key.getExclusiveToName() != null && key.getExclusiveToName().equalsIgnoreCase(player.getName()))
                .collect(Collectors.toList());
    }

    public void open(int page) {
        // Criar inventário com 6 linhas (54 slots) usando holder
        KeysMenuHolder holder = new KeysMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54, "Keys");

        // Obter todas as keys exclusivas do jogador
        List<Key> playerKeys = keyService.getAllKeys().stream()
                .filter(key -> key.getState() == KeyState.ATIVA)
                .filter(key -> key.getExclusiveToName() != null &&
                        key.getExclusiveToName().equalsIgnoreCase(player.getName()))
                .collect(Collectors.toList());

        // Calcular totalPages fora do bloco if/else para estar disponível globalmente
        int keysPerPage = 45;
        int totalPages = (int) Math.ceil((double) playerKeys.size() / keysPerPage);

        // Obter configurações
        FileConfiguration config = plugin.getConfig();
        String backgroundMaterial = config.getString("Items.BackgroundMaterial", "RED_STAINED_GLASS_PANE");
        String statusItemMaterial = config.getString("Items.StatusItemMaterial", "BOOK");
        String statusItemName = config.getString("Items.StatusItemName", "&aStatus das Keys");
        List<String> statusItemLore = config.getStringList("Items.StatusItemLore");
        String voucherItemMaterial = config.getString("Items.VoucherItemMaterial", "SUNFLOWER");

        // Configurações para item "sem keys"
        String noKeysMaterial = config.getString("Items.NoKeysItem.Material", "BARRIER");
        String noKeysName = config.getString("Items.NoKeysItem.Name", "&cNenhuma Key Disponível");
        List<String> noKeysLore = config.getStringList("Items.NoKeysItem.Lore");

        // Configurações para item de key
        String keyItemNameTemplate = config.getString("Items.KeyItem.Name", "&b{key}");
        List<String> keyItemLoreTemplate = config.getStringList("Items.KeyItem.Lore");

        // Criar item de background SEM NOME VISÍVEL
        ItemStack backgroundItem = new ItemStack(Material.matchMaterial(backgroundMaterial));
        ItemMeta backgroundMeta = backgroundItem.getItemMeta();
        backgroundMeta.setDisplayName(""); // Nome vazio - remove o nome padrão do material
        backgroundMeta.setLore(new ArrayList<>()); // Apenas lore vazia
        backgroundItem.setItemMeta(backgroundMeta);

        // Preencher apenas os slots de conteúdo (0-44) com background
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, backgroundItem);
        }

        if (playerKeys.isEmpty()) {
            // Jogador não tem keys - mostrar mensagem central
            ItemStack noKeysItem = createItem(
                    Material.matchMaterial(noKeysMaterial),
                    noKeysName,
                    noKeysLore
            );
            inventory.setItem(22, noKeysItem);
        } else {
            // Paginação das keys
            int startIndex = page * keysPerPage;
            int endIndex = Math.min(startIndex + keysPerPage, playerKeys.size());

            List<Key> keysToShow = playerKeys.subList(startIndex, endIndex);

            // Adicionar keys aos slots de conteúdo (0-44)
            for (int i = 0; i < keysToShow.size(); i++) {
                Key key = keysToShow.get(i);
                ItemStack keyItem = createKeyItem(key, keyItemNameTemplate, keyItemLoreTemplate);
                inventory.setItem(i, keyItem);
            }
        }

        // Atualizar barra de navegação (45-53) - agora os botões não serão sobrescritos
        updateNavigationButtons(
                inventory,
                page,
                totalPages,
                statusItemMaterial,
                statusItemName,
                statusItemLore,
                voucherItemMaterial,
                playerKeys
        );




        // Abrir inventário
        player.openInventory(inventory);

        // Armazenar página atual no plugin (temporariamente)
        plugin.getPlayerKeysGUIs().put(player.getUniqueId(), page);
    }

    private ItemStack createKeyItem(Key key, String nameTemplate, List<String> loreTemplate) {
        FileConfiguration config = plugin.getConfig();
        String typeKey = key.getTypeKey();

        // Obter informações do tipo de key
        String chatPrefix = config.getString("Types." + typeKey + ".ChatPrefix", "&7Desconhecido");
        String reward = config.getString("Types." + typeKey + ".Recompensa", "Recompensa desconhecida");
        String title = config.getString("Types." + typeKey + ".Title", "Key Desconhecida");

        // Criar item da key
        Material material = Material.matchMaterial(config.getString("Items.PlayerKeyItem", "TRIPWIRE_HOOK"));

        // Processar template de nome e lore
        String processedName = nameTemplate
                .replace("{key}", key.getCode())
                .replace("{type}", typeKey)
                .replace("{chatprefix}", chatPrefix)
                .replace("{reward}", reward)
                .replace("{title}", title);

        List<String> processedLore = new ArrayList<>();
        for (String loreLine : loreTemplate) {
            String processedLine = loreLine
                    .replace("{key}", key.getCode())
                    .replace("{type}", typeKey)
                    .replace("{chatprefix}", chatPrefix)
                    .replace("{reward}", reward)
                    .replace("{title}", title);
            processedLore.add(processedLine);
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.applyColor(processedName));
        meta.setLore(processedLore.stream().map(MessageUtils::applyColor).collect(java.util.stream.Collectors.toList()));

        // Armazenar o código da key no PDC
        NamespacedKey keyNamespaced = new NamespacedKey(plugin, "key_code");
        meta.getPersistentDataContainer().set(keyNamespaced, PersistentDataType.STRING, key.getCode());

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.applyColor(name));
        meta.setLore(lore.stream().map(MessageUtils::applyColor).collect(java.util.stream.Collectors.toList()));
        item.setItemMeta(meta);
        return item;
    }


    private void updateNavigationButtons(
            Inventory inventory,
            int currentPage,
            int totalPages,
            String statusItemMaterial,
            String statusItemName,
            List<String> statusItemLore,
            String voucherItemMaterial,
            List<Key> allPlayerKeys
    )
    {
    // SLOT 48 — PÁGINA ANTERIOR
        ItemStack prevButton = createNavigationButton(
                currentPage > 0 ? "LIME_DYE" : "GRAY_DYE",
                currentPage > 0 ? "&e◀ Página Anterior" : "&7◀ Página Anterior",
                null

        );
        inventory.setItem(48, prevButton);

        // SLOT 49 — INDICADOR DE PÁGINA ATUAL (INFORMATIVO)
        ItemStack pageIndicator = new ItemStack(Material.matchMaterial("BOOK")); // Material informativo
        ItemMeta pageMeta = pageIndicator.getItemMeta();
        pageMeta.setDisplayName(MessageUtils.applyColor("&7Página &f" + (currentPage + 1) + " &7de &f" + totalPages));
        pageIndicator.setItemMeta(pageMeta);
        inventory.setItem(49, pageIndicator);


        // SLOT 50 — PRÓXIMA PÁGINA
        ItemStack nextButton = createNavigationButton(
                currentPage < totalPages - 1 ? "LIME_DYE" : "GRAY_DYE",
                currentPage < totalPages - 1 ? "&ePróxima Página ▶" : "&7Próxima Página ▶",
                null

        );
        inventory.setItem(50, nextButton);

        // SLOT 45 — VOUCHER (mantido como estava)
        inventory.setItem(45, createVoucherButton(player, voucherItemMaterial));

        // SLOT 53 — Fechar menu (mantido como estava)
        ItemStack closeButton = createNavigationButton(
                "BARRIER",
                "&cFechar Menu",
                null
        );
        inventory.setItem(53, closeButton);
    }

    private ItemStack createNavigationButton(String material, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.matchMaterial(material));
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        meta.setDisplayName(MessageUtils.applyColor(name));

        // ✅ Se lore for null ou vazia, não seta lore
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore.stream()
                    .map(MessageUtils::applyColor)
                    .toList());
        }

        item.setItemMeta(meta);
        return item;
    }


    private ItemStack createVoucherButton(Player player, String materialName) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.SUNFLOWER;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtils.applyColor("&e&l\uD83D\uDCB0 Criar Voucher"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(MessageUtils.applyColor("&7Transforme suas keys em"));
        lore.add(MessageUtils.applyColor("&7item físico para presentear."));

        if (validationService.hasVoucherPermission(player)) {
            lore.add("");
            lore.add(MessageUtils.applyColor("&a▶ Clique para criar."));
        } else {
            lore.add("");
            lore.add(MessageUtils.applyColor("&c&l✖ Você não tem permissão"));
            lore.add(MessageUtils.applyColor("&7Precisa ser &bMVP &7ou &e&lELITE&7."));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }


}