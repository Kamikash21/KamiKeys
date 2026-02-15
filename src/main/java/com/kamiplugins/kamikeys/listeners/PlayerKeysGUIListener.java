package com.kamiplugins.kamikeys.listeners;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.commands.PlayerKeysCommand;
import com.kamiplugins.kamikeys.gui.PlayerVoucherKeySelectionGUI;
import com.kamiplugins.kamikeys.interfaces.ConfirmActivationHolder;
import com.kamiplugins.kamikeys.interfaces.ConfirmDeletionHolder;
import com.kamiplugins.kamikeys.interfaces.KeysMenuHolder;
import com.kamiplugins.kamikeys.managers.AuditLogger;
import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.models.enums.KeyState;
import com.kamiplugins.kamikeys.services.KeyService;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import com.kamiplugins.kamikeys.utils.MessageUtils;
import com.kamiplugins.kamikeys.utils.UxService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;



public class PlayerKeysGUIListener implements Listener {
    private final Main plugin;
    private final PlayerKeysCommand playerKeysCommand;
    private final KeyService keyService;
    private final UxService uxService;
    private final AuditLogger auditLogger;

    // Mapa local para armazenar ações pendentes
    private final Map<UUID, String> pendingActions = new HashMap<>();
    private final Map<UUID, String> pendingActionData = new HashMap<>();

    public PlayerKeysGUIListener(Main plugin, PlayerKeysCommand playerKeysCommand, KeyService keyService) {
        this.plugin = plugin;
        this.playerKeysCommand = playerKeysCommand;
        this.keyService = keyService;
        this.uxService = plugin.getConfigManager().getUxService();
        this.auditLogger  = plugin.getAuditLogger();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Identificar inventário pelo holder
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof ConfirmActivationHolder) {
            handleConfirmationMenuClick(event, player, "activate");
        } else if (holder instanceof ConfirmDeletionHolder) {
            handleConfirmationMenuClick(event, player, "delete");
        } else if (holder instanceof KeysMenuHolder) {
            handleKeysMenuClick(event, player);
        }
    }

    private void handleKeysMenuClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true); // Cancelar todas as ações padrão

        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();
        ClickType clickType = event.getClick();

        // Verificar se é um slot de navegação (45-53)
        if (slot >= 45 && slot <= 53) {
            handleNavigationClick(player, slot);
            return;
        }

        // Verificar se é o item "sem keys" (slot 22)
        if (slot == 22 && clickedItem != null && clickedItem.hasItemMeta() &&
                clickedItem.getItemMeta().getDisplayName().contains("Nenhuma Key")) {
            // Clicou no item de "sem keys" - tocar som de erro
            uxService.playSoundFromConfig(player, "key_off");
            return;
        }

        // Verificar se é um item de background
        String backgroundMaterial = plugin.getConfig().getString("Items.BackgroundMaterial", "RED_STAINED_GLASS_PANE");
        Material backgroundMat = Material.matchMaterial(backgroundMaterial);
        if (backgroundMat != null && clickedItem != null && clickedItem.getType() == backgroundMat) {
            return;
        }

        // Verificar se é um item real (não nulo e não background)
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return; // Não fazer nada se não houver item
        }

        // Etapa 1: Identificar corretamente o item como uma KEY via PDC
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta != null) {
            NamespacedKey keyNamespaced = new NamespacedKey(plugin, "key_code");
            String keyCode = meta.getPersistentDataContainer().get(keyNamespaced, PersistentDataType.STRING);

            if (keyCode == null) {
                // Não é uma key válida (não tem código armazenado no PDC)
                uxService.playSoundFromConfig(player, "error");
                return;
            }

            // Etapa 2: Buscar a key real no KeyService usando o código
            Key key = keyService.findByCode(keyCode).orElse(null);
            if (key == null) {
                // Key não encontrada - tocar som de erro
                uxService.sendError(player, "player.key_not_found", MessageUtils.createPlaceholders());
                uxService.playSoundFromConfig(player, "error");
                return;
            }

            // Etapa 3: Validar ANTES de abrir menu de confirmação:
            // ✔ A key EXISTE (já verificado acima)
            // ✔ A key pertence ao PLAYER clicando
            if (key.getExclusiveToName() == null || !key.getExclusiveToName().equalsIgnoreCase(player.getName())) {
                // Key não pertence ao jogador - tocar som de erro
                uxService.sendError(player, "player.key_not_yours", MessageUtils.createPlaceholders());
                uxService.playSoundFromConfig(player, "error");
                return;
            }

            // ✔ A key está no estado ATIVA
            if (key.getState() != KeyState.ATIVA) {
                // Key não está ativa - tocar som de erro
                uxService.sendError(player, "player.key_not_active", MessageUtils.createPlaceholders());
                uxService.playSoundFromConfig(player, "error");
                return;
            }

            // Etapa 4: Somente se TODAS as validações passarem:
            if (clickType == ClickType.LEFT) {
                // Clique esquerdo → abrir menu de CONFIRMAÇÃO DE ATIVAÇÃO
                openActivationConfirmationMenu(player, keyCode);
                uxService.playSoundFromConfig(player, "open_menu");
            } else if (clickType == ClickType.RIGHT) {
                // Clique direito → abrir menu de CONFIRMAÇÃO DE EXCLUSÃO
                openDeletionConfirmationMenu(player, keyCode);
                uxService.playSoundFromConfig(player, "open_menu");
            } else {
                // Outros cliques - tocar som de erro
                uxService.playSoundFromConfig(player, "error");
            }
        } else {
            // Item não tem metadata - tocar som de erro
            uxService.playSoundFromConfig(player, "error");
        }
    }

    private void handleConfirmationMenuClick(InventoryClickEvent event, Player player, String actionType) {
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Obter o holder do inventário
        InventoryHolder holder = event.getInventory().getHolder();

        // Verificar se é um holder de exclusão
        if (holder instanceof ConfirmDeletionHolder) {
            ConfirmDeletionHolder deletionHolder = (ConfirmDeletionHolder) holder;
            String keyCode = deletionHolder.getKeyCode();

            if (keyCode == null) {
                // Código da key não encontrado - cancelar ação
                uxService.playSoundFromConfig(player, "error");
                player.closeInventory();
                return;
            }

            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName() != null) {
                String displayName = clickedItem.getItemMeta().getDisplayName();

                if (displayName.contains("Confirmar")) {
                    // Confirmar exclusão
                    Key key = keyService.findByCode(keyCode).orElse(null);
                    if (key != null) {
                        // Validar novamente antes de excluir
                        if (key.getExclusiveToName() != null && key.getExclusiveToName().equalsIgnoreCase(player.getName()) &&
                                key.getState() == KeyState.ATIVA) {
                            // Marcar key como excluída (ou removê-la)
                            key.setState(KeyState.BLOQUEADA); // Ou outro estado de exclusão
                            keyService.updateKey(key);

                            // ENVIAR MENSAGEM ESPECÍFICA PARA EXCLUSÃO COM TITLE E ACTIONBAR
                            String successMessage = uxService.getMessage("player.key_deleted");
                            String processedMessage = MessageUtils.applyPlaceholders(successMessage, MessageUtils.createPlaceholders("key", keyCode));
                            String prefixPlugin = uxService.getMessage("general.prefix");
                            String finalMessage = prefixPlugin + " " + processedMessage;
                            player.sendMessage(MessageUtils.applyColor(finalMessage));

                            // Enviar title específico para exclusão (se configurado)
                            String deleteTitle = uxService.getMessage("activation.deletion_success.title");
                            String deleteSubtitle = uxService.getMessage("activation.deletion_success.subtitle");
                            String processedTitle = MessageUtils.applyPlaceholders(deleteTitle, MessageUtils.createPlaceholders("key", keyCode));
                            String processedSubtitle = MessageUtils.applyPlaceholders(deleteSubtitle, MessageUtils.createPlaceholders("key", keyCode));
                            player.sendTitle(
                                    MessageUtils.applyColor(processedTitle),
                                    MessageUtils.applyColor(processedSubtitle),
                                    10, 70, 40
                            );

                            // Enviar actionbar específico para exclusão
                            String deleteActionBar = uxService.getMessage("activation.deletion_success.actionbar");
                            String processedActionBar = MessageUtils.applyPlaceholders(deleteActionBar, MessageUtils.createPlaceholders("key", keyCode));
                            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                                            MessageUtils.applyColor(processedActionBar)
                                    )
                            );

                            uxService.playSoundFromConfig(player, "key_deleted");

                            // Atualizar menu principal com delay para evitar conflitos
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                Integer currentPage = (Integer) plugin.getPlayerKeysGUIs().get(player.getUniqueId());
                                if (currentPage != null) {
                                    // Garantir que a página atual ainda é válida após a exclusão
                                    int totalPages = getTotalPages(player);
                                    if (currentPage < totalPages) {
                                        playerKeysCommand.refreshKeysMenu(player, currentPage);
                                    } else {
                                        // Se a página atual for inválida, voltar para a última página válida
                                        int newPage = Math.max(0, totalPages - 1);
                                        playerKeysCommand.refreshKeysMenu(player, newPage);
                                        plugin.getPlayerKeysGUIs().put(player.getUniqueId(), newPage);
                                    }
                                }
                            }, 20L); // 1 segundo de delay

                            // Fechar inventário com delay
                            Bukkit.getScheduler().runTaskLater(plugin, player::closeInventory, 1L);

                            // Registrar auditoria de exclusão

                            String playerNick = (player != null) ? player.getName() : "UNKNOWN";
                            String playerIp = (player != null) ? player.getAddress().getHostString() : "UNKNOWN";


                            auditLogger.logKeyEvent(
                                    AuditLogger.AuditAction.EXCLUIDA,
                                    key.getCode(),
                                    key.getTypeKey(),
                                    key.getOrigin().name(),
                                    KeyState.ATIVA,
                                    KeyState.EXCLUIDA,
                                    AuditLogger.AuditActor.player(playerNick + " | UUID : " + player.getUniqueId().toString()),
                                    playerIp,
                                    AuditLogger.AuditSource.gui("Keys"),
                                    "Key excluida pelo jogador"
                            );

                        } else {
                            // Erro de validação - enviar mensagem de erro
                            String errorMessage = uxService.getMessage("player.key_not_yours");
                            String processedMessage = MessageUtils.applyPlaceholders(errorMessage, MessageUtils.createPlaceholders("key", keyCode));
                            String prefixPlugin = uxService.getMessage("general.prefix");
                            String finalMessage = prefixPlugin + " " + processedMessage;
                            player.sendMessage(MessageUtils.applyColor(finalMessage));

                            // Enviar title de erro
                            String errorTitle = uxService.getMessage("activation.error_key_not_yours.title");
                            String errorSubtitle = uxService.getMessage("activation.error_key_not_yours.subtitle");
                            String processedTitle = MessageUtils.applyPlaceholders(errorTitle, MessageUtils.createPlaceholders("key", keyCode));
                            String processedSubtitle = MessageUtils.applyPlaceholders(errorSubtitle, MessageUtils.createPlaceholders("key", keyCode));
                            player.sendTitle(
                                    MessageUtils.applyColor(processedTitle),
                                    MessageUtils.applyColor(processedSubtitle),
                                    10, 70, 20
                            );

                            // Enviar actionbar de erro
                            String errorActionBar = uxService.getMessage("activation.error_key_not_yours.actionbar");
                            String processedActionBar = MessageUtils.applyPlaceholders(errorActionBar, MessageUtils.createPlaceholders("key", keyCode));
                            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                                            MessageUtils.applyColor(processedActionBar)
                                    )
                            );

                            uxService.playSoundFromConfig(player, "error");

                            // Fechar inventário com delay
                            Bukkit.getScheduler().runTaskLater(plugin, player::closeInventory, 1L);
                        }
                    } else {
                        // Key não encontrada
                        String errorMessage = uxService.getMessage("player.key_not_found");
                        String processedMessage = MessageUtils.applyPlaceholders(errorMessage, MessageUtils.createPlaceholders("key", keyCode));
                        String prefixPlugin = uxService.getMessage("general.prefix");
                        String finalMessage = prefixPlugin + " " + processedMessage;
                        player.sendMessage(MessageUtils.applyColor(finalMessage));

                        // Enviar title de erro
                        String errorTitle = uxService.getMessage("activation.error_key.title");
                        String errorSubtitle = uxService.getMessage("activation.error_key.subtitle");
                        String processedTitle = MessageUtils.applyPlaceholders(errorTitle, MessageUtils.createPlaceholders("key", keyCode));
                        String processedSubtitle = MessageUtils.applyPlaceholders(errorSubtitle, MessageUtils.createPlaceholders("key", keyCode));
                        player.sendTitle(
                                MessageUtils.applyColor(processedTitle),
                                MessageUtils.applyColor(processedSubtitle),
                                10, 70, 20
                        );

                        // Enviar actionbar de erro
                        String errorActionBar = uxService.getMessage("activation.error_key.actionbar");
                        String processedActionBar = MessageUtils.applyPlaceholders(errorActionBar, MessageUtils.createPlaceholders("key", keyCode));
                        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                                        MessageUtils.applyColor(processedActionBar)
                                )
                        );

                        uxService.playSoundFromConfig(player, "error");

                        // Fechar inventário com delay
                        Bukkit.getScheduler().runTaskLater(plugin, player::closeInventory, 1L);
                    }
                } else if (displayName.contains("Cancelar")) {
                    // Cancelar ação
                    uxService.playSoundFromConfig(player, "cancel");

                    // Voltar ao menu principal
                    Integer currentPage = (Integer) plugin.getPlayerKeysGUIs().get(player.getUniqueId());
                    if (currentPage != null) {
                        // Garantir que a página atual ainda é válida após a exclusão
                        int totalPages = getTotalPages(player);
                        if (currentPage < totalPages) {
                            playerKeysCommand.refreshKeysMenu(player, currentPage);
                        } else {
                            // Se a página atual for inválida, voltar para a última página válida
                            int newPage = Math.max(0, totalPages - 1);
                            playerKeysCommand.refreshKeysMenu(player, newPage);
                            plugin.getPlayerKeysGUIs().put(player.getUniqueId(), newPage);
                        }
                    } else {
                        player.closeInventory();
                    }
                }
            }
        } else if (holder instanceof ConfirmActivationHolder) {
            // Confirmar ativação
            ConfirmActivationHolder activationHolder = (ConfirmActivationHolder) holder;
            String keyCode = activationHolder.getKeyCode();

            if (keyCode == null) {
                // Código da key não encontrado - cancelar ação
                uxService.playSoundFromConfig(player, "error");
                player.closeInventory();
                return;
            }

            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName() != null) {
                String displayName = clickedItem.getItemMeta().getDisplayName();

                if (displayName.contains("Confirmar")) {
                        // Chamar KeyService com UUID do jogador e tratar o resultado
                        java.util.Optional<Key> activated = keyService.activateKey(keyCode, player.getName(), player.getUniqueId());
                        if (activated.isEmpty()) {
                            // Falha na ativação (KeyService já registra motivos) -> feedback ao jogador
                            uxService.sendError(player, "player.activation_failed", MessageUtils.createPlaceholders());
                            uxService.playSoundFromConfig(player, "error");
                            // Fechar inventário com pequeno delay
                            Bukkit.getScheduler().runTaskLater(plugin, player::closeInventory, 1L);
                            return;
                        }

                        // Remover ações pendentes
                        pendingActions.remove(player.getUniqueId());
                        pendingActionData.remove(player.getUniqueId());
                        uxService.playSoundFromConfig(player, "success");

                        // Atualizar menu principal com delay
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            Integer currentPage = (Integer) plugin.getPlayerKeysGUIs().get(player.getUniqueId());
                            if (currentPage != null) {
                                playerKeysCommand.refreshKeysMenu(player, currentPage);
                            }
                        }, 20L);

                        // Fechar inventário com delay
                        Bukkit.getScheduler().runTaskLater(plugin, player::closeInventory, 1L);

                } else if (displayName.contains("Cancelar")) {
                    // Cancelar ação
                    pendingActions.remove(player.getUniqueId());
                    pendingActionData.remove(player.getUniqueId());
                    uxService.playSoundFromConfig(player, "cancel");

                    // Voltar ao menu principal
                    Integer currentPage = (Integer) plugin.getPlayerKeysGUIs().get(player.getUniqueId());
                    if (currentPage != null) {
                        playerKeysCommand.refreshKeysMenu(player, currentPage);
                    } else {
                        player.closeInventory();
                    }
                }
            }
        }
    }

    private void openActivationConfirmationMenu(Player player, String keyCode) {
        // Validar ANTES de abrir o menu
        Key key = keyService.findByCode(keyCode).orElse(null);
        if (key == null) {
            uxService.sendError(player, "player.key_not_found", MessageUtils.createPlaceholders());
            uxService.playSoundFromConfig(player, "error");
            return;
        }

        // Validar pertencimento e estado ANTES de abrir menu
        if (key.getExclusiveToName() == null || !key.getExclusiveToName().equalsIgnoreCase(player.getName())) {
            uxService.sendError(player, "player.key_not_yours", MessageUtils.createPlaceholders());
            uxService.playSoundFromConfig(player, "error");
            return;
        }

        if (key.getState() != KeyState.ATIVA) {
            uxService.sendError(player, "player.key_not_active", MessageUtils.createPlaceholders());
            uxService.playSoundFromConfig(player, "error");
            return;
        }

        // Criar inventário de confirmação de ativação usando holder com código da key
        ConfirmActivationHolder holder = new ConfirmActivationHolder(keyCode);
        Inventory confirmationInventory = Bukkit.createInventory(holder, 27, "Confirmar Ativação");

        FileConfiguration config = plugin.getConfig();
        String typeKey = key.getTypeKey();
        String chatPrefix = config.getString("Types." + typeKey + ".ChatPrefix", "&7Desconhecido");
        String reward = config.getString("Types." + typeKey + ".Recompensa", "Recompensa desconhecida");

        // Item de confirmação
        ItemStack confirmItem = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(MessageUtils.applyColor("&aConfirmar Ativação"));
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("");
        confirmLore.add("&7Key: &b" + keyCode);
        confirmLore.add("&7\uD83C\uDF81Recompensa: &e" + reward);
        confirmLore.add("");
        confirmLore.add("&aClique para confirmar");
        confirmMeta.setLore(confirmLore.stream().map(MessageUtils::applyColor).collect(java.util.stream.Collectors.toList()));
        confirmItem.setItemMeta(confirmMeta);

        // Item de cancelamento
        ItemStack cancelItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(MessageUtils.applyColor("&cCancelar"));
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add("&7Voltar ao menu anterior");
        cancelLore.add("&cClique para cancelar");
        cancelMeta.setLore(cancelLore.stream().map(MessageUtils::applyColor).collect(java.util.stream.Collectors.toList()));
        cancelItem.setItemMeta(cancelMeta);

        // Preencher inventário
        for (int i = 0; i < 27; i++) {
            if (i == 11) {
                confirmationInventory.setItem(i, confirmItem);
            } else if (i == 15) {
                confirmationInventory.setItem(i, cancelItem);
            } else {
                confirmationInventory.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
            }
        }

        // Abrir inventário de confirmação
        player.openInventory(confirmationInventory);

        // Armazenar a key para confirmação
        pendingActions.put(player.getUniqueId(), "activate_key");
        pendingActionData.put(player.getUniqueId(), keyCode);
    }

    private void openDeletionConfirmationMenu(Player player, String keyCode) {
        // Validar ANTES de abrir o menu
        Key key = keyService.findByCode(keyCode).orElse(null);
        if (key == null) {
            uxService.sendError(player, "player.key_not_found", MessageUtils.createPlaceholders());
            uxService.playSoundFromConfig(player, "error");
            return;
        }

        // Validar pertencimento e estado ANTES de abrir menu
        if (key.getExclusiveToName() == null || !key.getExclusiveToName().equalsIgnoreCase(player.getName())) {
            uxService.sendError(player, "player.key_not_yours", MessageUtils.createPlaceholders());
            uxService.playSoundFromConfig(player, "error");
            return;
        }

        if (key.getState() != KeyState.ATIVA) {
            uxService.sendError(player, "player.key_not_active", MessageUtils.createPlaceholders());
            uxService.playSoundFromConfig(player, "error");
            return;
        }

        // Criar inventário de confirmação de exclusão usando holder com código da key
        ConfirmDeletionHolder holder = new ConfirmDeletionHolder(keyCode);
        Inventory confirmationInventory = Bukkit.createInventory(holder, 27, "Confirmar Exclusão");

        FileConfiguration config = plugin.getConfig();
        String typeKey = key.getTypeKey();
        String chatPrefix = config.getString("Types." + typeKey + ".ChatPrefix", "&7Desconhecido");
        String reward = config.getString("Types." + typeKey + ".recompensa", "Recompensa desconhecida");

        // Item de confirmação
        ItemStack confirmItem = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(MessageUtils.applyColor("&cConfirmar Exclusão"));
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("");
        confirmLore.add("&7Key: &b" + keyCode);
        confirmLore.add("&7\uD83C\uDF81Recompensa: &e" + reward);
        confirmLore.add("");
        confirmLore.add("&e⚠Esta ação é irreversível!");
        confirmLore.add("&c&lClique para confirmar");
        confirmMeta.setLore(confirmLore.stream().map(MessageUtils::applyColor).collect(java.util.stream.Collectors.toList()));
        confirmItem.setItemMeta(confirmMeta);

        // Item de cancelamento
        ItemStack cancelItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(MessageUtils.applyColor("&aCancelar"));
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add("&7Voltar ao menu anterior");
        cancelLore.add("&aClique para cancelar");
        cancelMeta.setLore(cancelLore.stream().map(MessageUtils::applyColor).collect(java.util.stream.Collectors.toList()));
        cancelItem.setItemMeta(cancelMeta);

        // Preencher inventário
        for (int i = 0; i < 27; i++) {
            if (i == 11) {
                confirmationInventory.setItem(i, confirmItem);
            } else if (i == 15) {
                confirmationInventory.setItem(i, cancelItem);
            } else {
                confirmationInventory.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
            }
        }

        // Abrir inventário de confirmação
        player.openInventory(confirmationInventory);
    }

    private void handleNavigationClick(Player player, int slot) {
        switch (slot) {
            case 48: // Página anterior
                navigatePreviousPage(player);
                break;
            case 50: // Próxima página
                navigateNextPage(player);
                break;
            case 49: // Status/Informação
                refreshCurrentPage(player);
                break;
            case 53: // Fechar menu
                closeMenu(player);
                break;
            case 45: // Voucher
                handleVerifcationVoucher(player);
                break;
        }
    }

    private void handleVerifcationVoucher(Player player) {

        // 0) Verificar permissão
        if (!player.hasPermission("kamikeys.voucher.player")) {
            uxService.playSoundFromConfig(player, "error");

            player.sendMessage(" ");
            player.sendMessage(ColorUtils.translate("&c❌ Você não pode criar vouchers."));
            player.sendMessage(ColorUtils.translate("&7Apenas jogadores &bMVP &7ou &e&lELITE&7."));
            player.sendMessage(ColorUtils.translate("&7podem transformar keys em vouchers."));
            player.sendMessage(" ");

            player.closeInventory();
            return;
        }

        // 1) Buscar TODAS as keys exclusivas ATIVAS do player
        List<Key> activeKeys = keyService.getKeysForPlayer(player.getName()).stream()
                .filter(k -> k.getState() == KeyState.ATIVA)
                .toList();

        if (activeKeys.isEmpty()) {
            player.closeInventory();
            uxService.playSoundFromConfig(player, "error");

            player.sendMessage(" ");
            player.sendMessage(ColorUtils.translate("&c❌ Você não possui nenhuma key ativa."));
            player.sendMessage(ColorUtils.translate("&7Adquire uma key primeiro."));
            player.sendMessage(" ");

            return;
        }

        // 2) Abrir MENU DE SELEÇÃO DE KEY (PLAYER)
        PlayerVoucherKeySelectionGUI gui = new PlayerVoucherKeySelectionGUI(plugin, player, activeKeys);
        plugin.getPlayerVoucherGUIs().put(player.getUniqueId(), gui);
        gui.open();


        uxService.playSoundFromConfig(player, "open_menu");
    }



    private void navigatePreviousPage(Player player) {
        Integer currentPage = (Integer) plugin.getPlayerKeysGUIs().get(player.getUniqueId());
        if (currentPage != null && currentPage > 0) {
            int newPage = currentPage - 1;
            playerKeysCommand.refreshKeysMenu(player, newPage);
            plugin.getPlayerKeysGUIs().put(player.getUniqueId(), newPage);
            uxService.playSoundFromConfig(player, "confirm_success"); // Som neutro
        } else {
            // Primeira página - tocar som de erro
            uxService.playSoundFromConfig(player, "error");
        }
    }

    private void navigateNextPage(Player player) {
        // Obter total de páginas
        int totalPages = getTotalPages(player);

        Integer currentPage = (Integer) plugin.getPlayerKeysGUIs().get(player.getUniqueId());
        if (currentPage != null && currentPage < totalPages - 1) {
            int newPage = currentPage + 1;
            playerKeysCommand.refreshKeysMenu(player, newPage);
            plugin.getPlayerKeysGUIs().put(player.getUniqueId(), newPage);
            uxService.playSoundFromConfig(player, "confirm_success"); // Som neutro
        } else {
            // Última página - tocar som de erro
            uxService.playSoundFromConfig(player, "error");
        }
    }

    private void refreshCurrentPage(Player player) {
        Integer currentPage = (Integer) plugin.getPlayerKeysGUIs().get(player.getUniqueId());
        if (currentPage != null) {
            playerKeysCommand.refreshKeysMenu(player, currentPage);
            uxService.playSoundFromConfig(player, "reload"); // Som de recarga
        }
    }

    private void closeMenu(Player player) {
        player.closeInventory();
        plugin.getPlayerKeysGUIs().remove(player.getUniqueId()); // Limpar referência
        uxService.playSoundFromConfig(player, "close_menu"); // Som suave
    }

    private int getTotalPages(Player player) {
        // Obter todas as keys exclusivas do jogador
        var allKeys = keyService.getAllKeys().stream()
                .filter(key -> key.getState() == KeyState.ATIVA)
                .filter(key -> key.getExclusiveToName() != null &&
                        key.getExclusiveToName().equalsIgnoreCase(player.getName()))
                .collect(Collectors.toList());

        int keysPerPage = 45;
        return Math.max(1, (int) Math.ceil((double) allKeys.size() / keysPerPage));
    }
}

