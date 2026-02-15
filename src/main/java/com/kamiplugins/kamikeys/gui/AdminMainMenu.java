package com.kamiplugins.kamikeys.gui;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.models.enums.KeyOrigin;
import com.kamiplugins.kamikeys.models.enums.KeyState;
import com.kamiplugins.kamikeys.services.KeyService;
import com.kamiplugins.kamikeys.services.ValidationService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AdminMainMenu {

    private final Main plugin;
    private final Player player;
    private final KeyService keyService;
    private final ValidationService validationService;

    public AdminMainMenu(Main plugin, Player player, KeyService keyService, ValidationService validationService) {
        this.plugin = plugin;
        this.player = player;
        this.keyService = keyService;
        this.validationService = validationService;
    }

    public void open(Player player) {
        Inventory inventory = createMenu(player);
        player.openInventory(inventory);
    }

    public Inventory createMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, "Menu Admin - KamiKeys");

        List<Key> allKeys = keyService.getAllKeys();

        List<Key> visibleKeys = allKeys.stream()
                .filter(k ->
                        k.getState() == KeyState.ATIVA ||
                                k.getState() == KeyState.VENDA ||
                                k.getState() == KeyState.RESERVADA ||
                                k.getState() == KeyState.VENDIDA ||
                                k.getState() == KeyState.BLOQUEADA
                )
                .collect(Collectors.toList());


        // ===========================
        // Botão: Keys de Venda
        // ===========================
        ItemStack vendaItem = new ItemStack(Material.EMERALD);
        ItemMeta vendaMeta = vendaItem.getItemMeta();
        vendaMeta.setDisplayName("§aKeys de Venda");
        List<String> vendaLore = new ArrayList<>();
        vendaLore.add("");
        vendaLore.add("&7Visualizar e gerenciar");
        vendaLore.add("&7as keys geradas para venda.");
        vendaLore.add("");

        List<Key> vendaKeys = visibleKeys.stream()
                .filter(k -> k.getOrigin() == KeyOrigin.VENDA)
                .filter(k -> k.getState() == KeyState.VENDA)
                .collect(Collectors.toList());


        String vendaCount = String.valueOf(vendaKeys.size());
        String vendaLast = "Nenhuma key registrada";

        if (!vendaKeys.isEmpty()) {
            Key last = vendaKeys.stream()
                    .max(Comparator.comparing(Key::getCreatedAt))
                    .orElse(null);

            if (last != null) {
                vendaLast = last.getCode();
            }
        }

        vendaLore.add("&7Quantidade: &f" + vendaCount);
        vendaLore.add("&7Última key: &b" + vendaLast);
        vendaLore.add("");
        vendaLore.add("&eClique para abrir");
        vendaMeta.setLore(colorizeLore(vendaLore));
        vendaItem.setItemMeta(vendaMeta);
        inventory.setItem(11, vendaItem);

        // ===========================
        // Botão: Keys Internas
        // ===========================
        ItemStack internaItem = new ItemStack(Material.DIAMOND);
        ItemMeta internaMeta = internaItem.getItemMeta();
        internaMeta.setDisplayName("§bKeys Internas");
        List<String> internaLore = new ArrayList<>();
        internaLore.add("");
        internaLore.add("&7Visualizar e gerenciar");
        internaLore.add("&7as keys internas sem dono.");
        internaLore.add("");

        List<Key> internaKeys = visibleKeys.stream()
                .filter(k -> k.getOrigin() == KeyOrigin.INTERNA)
                .filter(k -> k.getState() == KeyState.ATIVA)
                .filter(k -> k.getExclusiveToName() == null)
                .collect(Collectors.toList());


        String internaCount = String.valueOf(internaKeys.size());
        String internaLast = "Nenhuma key registrada";

        if (!internaKeys.isEmpty()) {
            Key last = internaKeys.stream()
                    .max(Comparator.comparing(Key::getCreatedAt))
                    .orElse(null);

            if (last != null) {
                internaLast = last.getCode();
            }
        }

        internaLore.add("&7Quantidade: &f" + internaCount);
        internaLore.add("&7Última key: &b" + internaLast);
        internaLore.add("");
        internaLore.add("&eClique para abrir");
        internaMeta.setLore(colorizeLore(internaLore));
        internaItem.setItemMeta(internaMeta);
        inventory.setItem(13, internaItem);

        // ===========================
        // Botão: Keys Exclusivas
        // ===========================
        ItemStack exclusivaItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta exclusivaMeta = exclusivaItem.getItemMeta();
        exclusivaMeta.setDisplayName("§cKeys Exclusivas");
        List<String> exclusivaLore = new ArrayList<>();
        exclusivaLore.add("");
        exclusivaLore.add("&7Visualizar e gerenciar");
        exclusivaLore.add("&7as keys ativas com dono.");
        exclusivaLore.add("");

        List<Key> exclusivaKeys = visibleKeys.stream()
                .filter(k -> k.getOrigin() == KeyOrigin.PLAYER)
                .filter(k -> k.getState() == KeyState.ATIVA || k.getState() == KeyState.VENDIDA)
                .filter(k -> k.getExclusiveToName() != null)
                .collect(Collectors.toList());


        String exclusivaCount = String.valueOf(exclusivaKeys.size());
        String exclusivaLast = "Nenhuma key registrada";

        if (!exclusivaKeys.isEmpty()) {
            Key last = exclusivaKeys.stream()
                    .max(Comparator.comparing(Key::getCreatedAt))
                    .orElse(null);

            if (last != null) {
                exclusivaLast = last.getCode();
            }
        }

        exclusivaLore.add("&7Quantidade: &f" + exclusivaCount);
        exclusivaLore.add("&7Última key: &b" + exclusivaLast);
        exclusivaLore.add("");
        exclusivaLore.add("&eClique para abrir");
        exclusivaMeta.setLore(colorizeLore(exclusivaLore));
        exclusivaItem.setItemMeta(exclusivaMeta);
        inventory.setItem(15, exclusivaItem);

        // ===========================
        // Botão: Todas as Keys
        // ===========================
        ItemStack todasItem = new ItemStack(Material.CHEST);
        ItemMeta todasMeta = todasItem.getItemMeta();
        todasMeta.setDisplayName("§6Todas as Keys");
        List<String> todasLore = new ArrayList<>();
        todasLore.add("");
        todasLore.add("&7Visualizar e gerenciar");
        todasLore.add("&7todas as keys ativas do sistema.");
        todasLore.add("");

        String todasCount = String.valueOf(visibleKeys.size());

        String todasLast = "Nenhuma key registrada";

        if (!visibleKeys.isEmpty()) {
            Key last = visibleKeys.stream()
                    .max(Comparator.comparing(Key::getCreatedAt))
                    .orElse(null);

            if (last != null) {
                todasLast = last.getCode();
            }
        }

        todasLore.add("&7Quantidade: &f" + todasCount);
        todasLore.add("&7Última key: &b" + todasLast);
        todasLore.add("");
        todasLore.add("&eClique para abrir");
        todasMeta.setLore(colorizeLore(todasLore));
        todasItem.setItemMeta(todasMeta);
        inventory.setItem(21, todasItem);

        // ===========================
        // Botão: Vouchers
        // ===========================

        ItemStack voucherItem = new ItemStack(Material.SUNFLOWER);
        ItemMeta voucherMeta = voucherItem.getItemMeta();
        voucherMeta.setDisplayName("§eCriar Voucher");
        List<String> voucherLore = new ArrayList<>();
        voucherLore.add("");
        voucherLore.add("&7Transforma uma key em um");
        voucherLore.add("&7voucher físico para o jogador.");
        voucherLore.add("");
        voucherLore.add("&eClique para abrir");
        voucherMeta.setLore(colorizeLore(voucherLore));
        voucherItem.setItemMeta(voucherMeta);
        inventory.setItem(23, voucherItem);


        // ===========================
        // Botão: Fechar
        // ===========================
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§cFechar");
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(40, closeItem);

        // ===========================
        // Decoração (vidro)
        // ===========================
        ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassPane.getItemMeta();
        glassMeta.setDisplayName(null);
        glassPane.setItemMeta(glassMeta);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glassPane);
            }
        }

        return inventory;
    }

    private List<String> colorizeLore(List<String> lore) {
        return lore.stream()
                .map(line -> line.replace("&", "§"))
                .collect(Collectors.toList());
    }
}
