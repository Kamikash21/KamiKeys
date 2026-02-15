package com.kamiplugins.kamikeys.commands;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.gui.AdminMainMenu;
import com.kamiplugins.kamikeys.managers.AuditLogger;
import com.kamiplugins.kamikeys.managers.BackupManager;
import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.models.enums.KeyOrigin;
import com.kamiplugins.kamikeys.models.enums.KeyState;
import com.kamiplugins.kamikeys.utils.KeyBatchDeleteResult;
import com.kamiplugins.kamikeys.utils.MessageUtils;
import com.kamiplugins.kamikeys.utils.UxService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.kamiplugins.kamikeys.models.enums.KeyOrigin.INTERNA;

public class KamiKeysCommand implements CommandExecutor, TabCompleter, Listener {
    private final Main plugin;
    private final UxService uxService;
    private final Map<UUID, Long> pendingVendaConfirmUntil = new HashMap<>();
    private final Map<UUID, Long> vendaCooldownUntil = new HashMap<>();
    private final Map<UUID, Long> playerAllCooldownUntil = new HashMap<>();
    private static final long PLAYER_ALL_COOLDOWN_MS = 60_000;
    private final Map<UUID, Long> pendingPlayerAllConfirmUntil = new HashMap<>();
    private static final long PLAYER_ALL_CONFIRM_WINDOW_MS = 15_000;
    private final Map<UUID, Long> pendingDeleteAllConfirmUntil = new HashMap<>();
    private static final long DELETE_ALL_CONFIRM_WINDOW_MS = 20_000;
    private static final long DELETE_ALL_COOLDOWN_MS = 120_000;
    private final BackupManager backupManager;
    private final AuditLogger auditLogger;


    public KamiKeysCommand(Main plugin) {
        this.plugin = plugin;
        this.uxService = plugin.getConfigManager().getUxService();
        this.backupManager = new BackupManager(plugin);
        this.auditLogger = new AuditLogger(plugin);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private static final long VENDA_CONFIRM_WINDOW_MS = 15_000; // 15s para confirmar
    private static final long VENDA_COOLDOWN_MS = 30_000;       // 30s cooldown anti spam


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kamikeys.admin.staff")) {
            uxService.sendError(sender, "common.no_permission", MessageUtils.createPlaceholders());
            uxService.playSoundFromConfig(sender, "error");
            return true;
        }

        if (args.length == 0) {
            // Mostrar help
            uxService.sendAdmin(sender, "admin.help", MessageUtils.createPlaceholders());
            return true;
        }

        String subcommand = args[0];

        switch (subcommand) {
            case "help":
                handleHelp(sender);
                break;
            case "list":
                handleList(sender);
                break;
            case "gerar":
                handleGerar(sender, args);
                break;
            case "dar":
                handleDar(sender, args);
                break;
            case "apagar":
                handleApagar(sender, args);
                break;
            case "backup":
                handleBackup(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            default:
                uxService.sendError(sender, "common.error", MessageUtils.createPlaceholders("details", "Subcomando desconhecado. Use /kamikeys help para ver os comandos disponíveis."));
                uxService.playSoundFromConfig(sender, "error");
                break;
        }



        return true;
    }

    private void handleHelp(CommandSender sender) {
        uxService.sendAdmin(sender, "admin.help", MessageUtils.createPlaceholders());
    }

    private void handleList(CommandSender sender) {
        if (!(sender instanceof Player)) {
            uxService.sendError(sender, "common.error",
                    MessageUtils.createPlaceholders("details", "Apenas jogadores podem usar este comando."));
            uxService.playSoundFromConfig(sender, "error");
            return;
        }

        Player player = (Player) sender;

        // Abrir o menu ADM usando a assinatura REAL do seu AdminMainMenu
        AdminMainMenu menu = new AdminMainMenu(
                plugin,
                player,
                plugin.getKeyService(),
                plugin.getValidationService()
        );

        uxService.playSoundFromConfig(sender, "open_menu");
        menu.open(player);
    }






    private void handleGerar(CommandSender sender, String[] args) {
        if (args.length < 4) {
            uxService.sendError(sender, "common.usage", MessageUtils.createPlaceholders("usage", "/kamikeys gerar <venda|interna> <tipo> <quantidade>"));
            uxService.playSoundFromConfig(sender, "error");
            return;
        }

        // Validar origem
        String originStr = args[1];
        KeyOrigin origin;
        if ("venda".equals(originStr)) {
            origin = KeyOrigin.VENDA;
        } else if ("interna".equals(originStr)) {
            origin = INTERNA;
        } else {
            uxService.sendError(sender, "common.error", MessageUtils.createPlaceholders("details", "Origem inválida. Use: venda ou interna"));
            uxService.playSoundFromConfig(sender, "error");
            return;
        }

        // Validar tipo (dinâmico)
        String typeKey = args[2];
        FileConfiguration config = plugin.getConfig();
        String configPath = "Types." + typeKey;


        if (!config.contains(configPath)) {
            uxService.sendError(sender, "common.error", MessageUtils.createPlaceholders("details", "Tipo inválido ou não configurado: " + typeKey));
            uxService.playSoundFromConfig(sender, "error");
            return;
        }

        // Validar quantidade
        int quantity;
        try {
            quantity = Integer.parseInt(args[3]);
            if (quantity <= 0 || quantity > 1000) {
                uxService.sendError(sender, "common.error", MessageUtils.createPlaceholders("details", "Quantidade deve ser entre 1 e 1000"));
                uxService.playSoundFromConfig(sender, "error");
                return;
            }
        } catch (NumberFormatException e) {
            uxService.sendError(sender, "common.error", MessageUtils.createPlaceholders("details", "Quantidade inválida"));
            uxService.playSoundFromConfig(sender, "error");
            return;
        }

        // Gerar as keys
        List<String> generatedKeys = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            Key key = plugin.getKeyService().generateKey(origin, typeKey, sender.getName(), (sender instanceof Player) ? (Player) sender : null);
            generatedKeys.add(key.getCode());
        }

        // Obter prefixo do tipo
        String typePrefix = config.getString("Types." + typeKey + ".ChatPrefix", "&e[" + typeKey + "]");

        // Obter prefixo do plugin
        String prefixPlugin = uxService.getMessage("general.prefix");

        // Converter origem para exibição
        String originDisplay = origin == KeyOrigin.VENDA ? "Venda" : "Interna";

        // Montar placeholders para a mensagem de resumo
        Map<String, String> placeholders = MessageUtils.createPlaceholders(
                "prefixplugin", prefixPlugin,
                "count", String.valueOf(quantity),
                "tipo", typePrefix,
                "origem", originDisplay
        );

        // Enviar mensagem de sucesso com placeholders
        String mensagemResumo = MessageUtils.applyPlaceholders(uxService.getMessage("admin.generation_done"), placeholders);
        String mensagemResumoComCores = MessageUtils.applyColor(mensagemResumo);

        // Enviar como admin (com prefixo e som)
        if (sender instanceof Player) {
            Player player = (Player) sender;
            // Aplica o prefixo de admin manualmente (já incluso na mensagem)
            player.sendMessage(mensagemResumoComCores);
            // Tocar som de admin se habilitado
            if (plugin.getConfig().getBoolean("Feedback.UseSounds", true)) {
                if (plugin.getConfig().getBoolean("sounds.admin.enabled", true)) {
                    String soundName = plugin.getConfig().getString("sounds.admin.sound", "ENTITY_ENDER_DRAGON_GROWL");
                    float volume = (float) plugin.getConfig().getDouble("sounds.admin.volume", 1.0);
                    float pitch = (float) plugin.getConfig().getDouble("sounds.admin.pitch", 1.0);
                    try {
                        player.playSound(player.getLocation(), soundName, volume, pitch);
                    } catch (Exception e) {
                        // Silently fail if sound is invalid
                    }
                }
            }
        } else {
            // Para console, enviar sem prefixo e sem cores
            sender.sendMessage(org.bukkit.ChatColor.stripColor(mensagemResumoComCores));
        }

        // Enviar lista de keys com prefixo
        sendKeyList(sender, generatedKeys);

        // Tocar som de sucesso configurado para gerar
        uxService.playSoundFromConfig(sender, "gerar");
    }

    private void sendKeyList(CommandSender sender, List<String> keys) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Enviar cada key como componente clicável
            for (String key : keys) {
                // Montar placeholders para a linha da key
                Map<String, String> placeholders = MessageUtils.createPlaceholders("key", key);
                String keyLineTemplate = uxService.getMessage("admin.generation_key_line");
                String keyLine = MessageUtils.applyPlaceholders(keyLineTemplate, placeholders);
                String keyLineWithColors = MessageUtils.applyColor(keyLine);

                net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(keyLineWithColors);

                // Tooltip do hover
                String hoverText = MessageUtils.applyColor(uxService.getMessage("admin.generation_key_hover"));
                msg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                        net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new net.md_5.bungee.api.chat.BaseComponent[]{
                                new net.md_5.bungee.api.chat.TextComponent(hoverText)
                        }
                ));

                // Click event para sugerir a key
                msg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND,
                        key
                ));

                player.spigot().sendMessage(msg);
            }
        } else {
            // Para console, enviar como texto simples
            for (String key : keys) {
                Map<String, String> placeholders = MessageUtils.createPlaceholders("key", key);
                String keyLineTemplate = uxService.getMessage("admin.generation_key_line");
                String keyLine = MessageUtils.applyPlaceholders(keyLineTemplate, placeholders);
                String keyLineWithColors = MessageUtils.applyColor(keyLine);

                sender.sendMessage(org.bukkit.ChatColor.stripColor(keyLineWithColors));
            }
        }
    }

    private void handleDar(CommandSender sender, String[] args) {
        if (args.length < 3) {
            uxService.sendError(sender, "common.usage", MessageUtils.createPlaceholders("usage", "/kamikeys dar <player> <tipo>"));
            uxService.playSoundFromConfig(sender, "error");
            return;
        }

        String playerName = args[1];
        String typeKey = args[2];

        // Validar tipo (dinâmico)
        FileConfiguration config = plugin.getConfig();
        String configPath = "Types." + typeKey;
        if (!config.contains(configPath)) {
            uxService.sendError(sender, "common.error", MessageUtils.createPlaceholders("details", "Tipo inválido ou não configurado: &f" + typeKey));
            uxService.playSoundFromConfig(sender, "error");
            return;
        }

        // Verificar se o jogador já entrou no servidor antes
        UUID playerUUID = null;
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer != null) {
            playerUUID = targetPlayer.getUniqueId();
        } else {
            // Tentar encontrar o UUID mesmo offline
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer.hasPlayedBefore()) {
                playerUUID = offlinePlayer.getUniqueId();
            } else {
                uxService.sendError(sender, "common.error", MessageUtils.createPlaceholders("details", "&cJogador &f" + playerName + " &cnão encontrado ou nunca entrou no servidor."));
                uxService.playSoundFromConfig(sender, "error");
                return;
            }
        }

        // Gerar key como INTERNA
        Key key = plugin.getKeyService().generateKey(KeyOrigin.PLAYER, typeKey, sender.getName(), (sender instanceof Player) ? (Player) sender : null);

        // Converter em exclusiva para o jogador
        boolean success = plugin.getKeyService().assignExclusiveKey(key.getCode(), playerName, playerUUID.toString());
        if (!success) {
            uxService.sendError(sender, "common.error", MessageUtils.createPlaceholders("details", "Erro ao atribuir key exclusiva para o jogador"));
            uxService.playSoundFromConfig(sender, "error");
            return;
        }

        // Obter prefixo do tipo
        String typePrefix = config.getString("Types." + typeKey + ".ChatPrefix", "&e[" + typeKey + "]");

        // Obter recompensa do tipo
        String reward = config.getString("Types." + typeKey + ".Recompensa", "Recompensa desconhecida");

        // Obter prefixo do plugin
        String prefixPlugin = uxService.getMessage("general.prefix");

        // Montar placeholders para a mensagem de sucesso (admin)
        Map<String, String> adminPlaceholders = MessageUtils.createPlaceholders(
                "prefixplugin", prefixPlugin,
                "tipo", typePrefix,
                "reward", reward,
                "player", playerName,
                "key", key.getCode()
        );

        // Enviar mensagem de sucesso para o admin
        String adminMessage = MessageUtils.applyPlaceholders(uxService.getMessage("admin.dar_done"), adminPlaceholders);
        String adminMessageWithColors = MessageUtils.applyColor(adminMessage);

        if (sender instanceof Player) {
            Player admin = (Player) sender;
            admin.sendMessage(adminMessageWithColors);

            // Tocar som de sucesso para o admin
            uxService.playSoundFromConfig(sender, "dar_admin");
        } else {
            // Para console, enviar sem cores
            sender.sendMessage(org.bukkit.ChatColor.stripColor(adminMessageWithColors));
        }

        // Enviar mensagem para o jogador (se online)
        if (targetPlayer != null && targetPlayer.isOnline()) {
            Map<String, String> playerPlaceholders = MessageUtils.createPlaceholders("reward", reward);
            String playerMessage = MessageUtils.applyPlaceholders(uxService.getMessage("player.key_received"), playerPlaceholders);
            String playerMessageWithColors = MessageUtils.applyColor(playerMessage);

            targetPlayer.sendMessage(playerMessageWithColors);

            // Tocar som para o jogador
            uxService.playSoundFromConfig(targetPlayer, "dar_player");
        }
    }

    private void handleApagar(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kamikeys.admin.*")) {
            uxService.sendError(sender, "common.no_permission", MessageUtils.createPlaceholders());
            uxService.playSoundFromConfig(sender, "error");
            return;
        }

        if (args.length < 2) {
            uxService.sendError(sender, "common.usage", MessageUtils.createPlaceholders("usage", "/kamikeys apagar <origem|tipo|player|tudo> [filtro]"));
            uxService.playSoundFromConfig(sender, "error");
            return;
        }

        String mode = args[1];
        String filter = args.length > 2 ? args[2] : null;

        // Verificar se já existe uma ação pendente
        UUID playerId = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        if (playerId != null && plugin.getPendingActionManager().hasPendingAction(playerId)) {
            uxService.sendError(sender, "common.error", MessageUtils.createPlaceholders("details", "Você já tem uma ação pendente. Aguarde para usar este comando novamente."));
            uxService.playSoundFromConfig(sender, "error");
            return;
        }

        String target;
        String filterValue = null;

        switch (mode) {
            case "origem":
                if (filter == null || (!filter.equals("venda") && !filter.equals("interna"))) {
                    uxService.sendError(sender, "common.error",
                            MessageUtils.createPlaceholders("details", "Origem inválida. &7Use: &fvenda ou interna"));
                    uxService.playSoundFromConfig(sender, "error");
                    return;
                }

                target = "ORIGEM";
                filterValue = filter;

                // 👉 PASSO 4: abrir GUI para origem interna
                if ("interna".equalsIgnoreCase(filter)) {

                    if (!(sender instanceof Player)) {
                        uxService.sendError(sender, "common.error",
                                MessageUtils.createPlaceholders("details", "Apenas jogadores podem usar este comando."));
                        return;
                    }

                    Player player = (Player) sender;
                    UUID pid = player.getUniqueId();

                    // 1️⃣ cria pending action (igual já faz hoje)
                    plugin.getPendingActionManager().createAction(
                            pid,
                            "DELETE_KEYS",
                            target,
                            filterValue
                    );

                    // 2️⃣ abre a GUI (texto específico)
                    openDeleteConfirmationGUI(
                            player,
                            "Apagar keys - Origem §bINTERNA",
                            List.of(
                                    " ",
                                    "§7Você está prestes a apagar todas as keys:",
                                    "§f• Origem: §bINTERNA",
                                    "§f• Status: §aATIVA",
                                    "",
                                    "§cEssa ação não pode ser desfeita"
                            )
                    );

                    return; // ⛔ NÃO deixa cair no fluxo antigo
                }

                // ✅ ORIGEM VENDA: confirmação no CHAT + cooldown (sem GUI)
                if ("venda".equalsIgnoreCase(filter)) {

                    if (!(sender instanceof Player)) {
                        uxService.sendError(sender, "common.error",
                                MessageUtils.createPlaceholders("details", "Apenas jogadores podem usar este comando."));
                        return;
                    }

                    Player player = (Player) sender;
                    UUID pid = player.getUniqueId();
                    long now = System.currentTimeMillis();

                    // 1) cooldown anti-spam
                    Long cdUntil = vendaCooldownUntil.get(pid);
                    if (cdUntil != null && cdUntil > now) {
                        long secondsLeft = (cdUntil - now) / 1000;
                        player.sendMessage("§cAguarde §f" + secondsLeft + "s §cpara usar este comando novamente.");
                        uxService.playSoundFromConfig(player, "error");
                        return;
                    }

                    // 2) janela de confirmação (2 passos)
                    Long confirmUntil = pendingVendaConfirmUntil.get(pid);
                    if (confirmUntil == null || confirmUntil <= now) {

                        // cria janela de confirmação
                        pendingVendaConfirmUntil.put(pid, now + VENDA_CONFIRM_WINDOW_MS);

                        // aviso forte no chat
                        player.sendMessage(" ");
                        player.sendMessage("§e§l⚠ §c§lATENÇÃO — AÇÃO DESTRUTIVA");
                        player.sendMessage("§7Você está prestes a apagar §cTODAS §7as keys de §2VENDA§7.");
                        player.sendMessage("§7Digite novamente em §f15s§7 para confirmar:");
                        player.sendMessage("§e/kamikeys apagar origem venda");
                        player.sendMessage(" ");

                        uxService.playSoundFromConfig(sender, "apagar_venda");
                        return;
                    }

                    // 3) confirmado (segunda execução dentro da janela)
                    pendingVendaConfirmUntil.remove(pid);
                    vendaCooldownUntil.put(pid, now + VENDA_COOLDOWN_MS);

                    String actor = player.getName();

                    // Busca keys da ORIGEM VENDA e filtra apenas estados permitidos
                    List<Key> keys = plugin.getKeyService()
                            .getKeysByOrigin(KeyOrigin.VENDA)
                            .stream()
                            .filter(k -> k.getState() == KeyState.VENDA || k.getState() == KeyState.ATIVA) // <- aqui está o ponto
                            .toList();

                    if (keys.isEmpty()) {
                        player.sendMessage("§e⚠ Nenhuma key de §2VENDA §eencontrada com status §aVENDA/ATIVA§e para apagar.");
                        uxService.playSoundFromConfig(sender, "error");
                        return;
                    }

                    // exclusão segura (KeyService decide o que pode apagar)
                    var result = plugin.getKeyService().deleteBatch(
                            keys,
                            actor,
                            "/kamikeys apagar origem venda"
                    );

                    player.sendMessage(" ");
                    player.sendMessage("§a✔ §cTodas as keys de origem §2VENDA§c foram processadas. " + " §7| Apagadas: §f" + result.getApagadas() + " §7| Ignoradas: §f" + result.getIgnoradasPorEstado());
                    player.sendMessage(" ");

                    //Title para player confirmando keys de venda apagadas
                    if (sender instanceof Player) {
                        player.sendTitle(
                                "§c§lTODAS AS KEYS APAGADAS",
                                "§7Confira o chat para detalhes",
                                10, 70, 20
                        );
                    }

                    // remover pending action após execução bem-sucedida
                    if (sender instanceof Player) {
                        plugin.getPendingActionManager().cancelAction(
                                ((Player) sender).getUniqueId()
                        );
                    }


                    uxService.playSoundFromConfig(sender, "confirm_success");
                    return;
                }

                break;

            case "tipo": {

                if (filter == null) {
                    uxService.sendError(sender, "common.error",
                            MessageUtils.createPlaceholders("details", "Tipo não especificado"));
                    uxService.playSoundFromConfig(sender, "error");
                    return;
                }

                if (!(sender instanceof Player)) {
                    uxService.sendError(sender, "common.error",
                            MessageUtils.createPlaceholders("details", "Apenas jogadores podem usar este comando."));
                    return;
                }

                // valida tipo na config
                FileConfiguration config = plugin.getConfig();
                String configPath = "Types." + filter;
                if (!config.contains(configPath)) {
                    uxService.sendError(sender, "common.error",
                            MessageUtils.createPlaceholders("details", "Tipo inválido ou não configurado: " + filter));
                    uxService.playSoundFromConfig(sender, "error");
                    return;
                }

                Player player = (Player) sender;
                UUID pid = player.getUniqueId();

                // cria pending action
                plugin.getPendingActionManager().createAction(
                        pid,
                        "DELETE_KEYS",
                        "TIPO",
                        filter
                );

                // abre GUI básica
                openDeleteConfirmationGUI(
                        player,
                        "Apagar keys - Tipo " + filter,
                        List.of(
                                " ",
                                "§7Você está prestes a apagar as keys:",
                                "§f• Tipo: §e" + filter,
                                "§f• Status: §aATIVA",
                                "",
                                "§cEssa ação não pode ser desfeita"
                        )
                );

                return;
            }

            case "player": {

                if (filter == null) {
                    uxService.sendError(sender, "common.error",
                            MessageUtils.createPlaceholders("details", "Nome do jogador não especificado"));
                    uxService.playSoundFromConfig(sender, "error");
                    return;
                }

                if (!(sender instanceof Player)) {
                    uxService.sendError(sender, "common.error",
                            MessageUtils.createPlaceholders("details", "Apenas jogadores podem usar este comando."));
                    return;
                }

                Player player = (Player) sender;
                UUID pid = player.getUniqueId();

                // ===== PLAYER ALL =====
                if ("all".equalsIgnoreCase(filter)) {
                    long now = System.currentTimeMillis();

                    // 1️⃣ cooldown
                    Long cdUntil = playerAllCooldownUntil.get(pid);
                    if (cdUntil != null && cdUntil > now) {
                        long secondsLeft = (cdUntil - now) / 1000;
                        player.sendMessage("§c⏳ Aguarde §f" + secondsLeft + "s §cpara usar este comando novamente.");
                        uxService.playSoundFromConfig(player, "error");
                        return;
                    }

                    // 2️⃣ janela de confirmação
                    Long confirmUntil = pendingPlayerAllConfirmUntil.get(pid);
                    if (confirmUntil == null || confirmUntil <= now) {

                        pendingPlayerAllConfirmUntil.put(pid, now + PLAYER_ALL_CONFIRM_WINDOW_MS);

                        player.sendMessage(" ");
                        player.sendMessage("§e§l⚠ §4§lATENÇÃO — AÇÃO EXTREMAMENTE PERIGOSA");
                        player.sendMessage("§7Você está prestes a apagar:");
                        player.sendMessage("§c• todas as keys de §4TODOS §cos jogadores");
                        player.sendMessage("§c• esta ação §4NÃO §cpode ser desfeita");
                        player.sendMessage(" ");
                        player.sendMessage("§7Digite novamente em §f15s§7 para confirmar:");
                        player.sendMessage("§e/kamikeys apagar player all");
                        player.sendMessage(" ");

                        uxService.playSoundFromConfig(player, "apagar_all");
                        return;
                    }

                    // 3️⃣ confirmação final
                    pendingPlayerAllConfirmUntil.remove(pid);
                    playerAllCooldownUntil.put(pid, now + PLAYER_ALL_COOLDOWN_MS);

                    plugin.getPendingActionManager().createAction(
                            pid,
                            "DELETE_KEYS",
                            "PLAYER_ALL",
                            null
                    );

                    executeDeleteAction(player, "PLAYER_ALL", null);
                    uxService.playSoundFromConfig(sender, "confirm_success");
                    return;
                }



                // ===== PLAYER ESPECÍFICO =====
                plugin.getPendingActionManager().createAction(
                        pid,
                        "DELETE_KEYS",
                        "PLAYER",
                        filter
                );

                openDeleteConfirmationGUI(
                        player,
                        "Apagar keys - Jogador",
                        List.of(
                                " ",
                                "§7Você está prestes a apagar as keys do:",
                                "§f• Jogador: §e" + filter,
                                "",
                                "§cEssa ação não pode ser desfeita"
                        )
                );
                return;
            }

            case "tudo": {

                if (!(sender instanceof Player)) {
                    uxService.sendError(sender, "common.error",
                            MessageUtils.createPlaceholders("details", "Apenas jogadores podem usar este comando."));
                    return;
                }

                Player player = (Player) sender;
                UUID pid = player.getUniqueId();
                long now = System.currentTimeMillis();

                Long confirmUntil = pendingDeleteAllConfirmUntil.get(pid);
                if (confirmUntil == null || confirmUntil <= now) {

                    pendingDeleteAllConfirmUntil.put(pid, now + DELETE_ALL_CONFIRM_WINDOW_MS);

                    player.sendMessage(" ");
                    player.sendMessage("§4§l☠ AÇÃO IRREVERSÍVEL");
                    player.sendMessage("§7Você está prestes a apagar:");
                    player.sendMessage("§c• §4TODAS §cas keys do servidor");
                    player.sendMessage("§c• Todas as origens, tipos e players");
                    player.sendMessage("§7 Confirme apenas se tiver certeza absoluta do que está fazendo");
                    player.sendMessage(" ");
                    player.sendMessage("§4§lESSA AÇÃO NÃO TEM VOLTA");
                    player.sendMessage(" ");
                    player.sendMessage("§7Digite novamente em §f20s§7 para confirmar:");
                    player.sendMessage("§e/kamikeys apagar tudo");
                    player.sendMessage(" ");

                    uxService.playSoundFromConfig(player, "apagar_tudo");
                    return;
                }

                pendingDeleteAllConfirmUntil.remove(pid);

                plugin.getPendingActionManager().createAction(
                        pid,
                        "DELETE_KEYS",
                        "TUDO",
                        null
                );

                executeDeleteAction(player, "TUDO", null);
                uxService.playSoundFromConfig(sender, "key_deleted");
                return;
            }


            default:
                uxService.sendError(sender, "common.error", MessageUtils.createPlaceholders("details", "Modo inválido. &7Use: &forigem, tipo, player ou tudo"));
                uxService.playSoundFromConfig(sender, "error");
                return;
        }
    }

    private void openDeleteConfirmationGUI(
            Player player,
            String title,
            List<String> descriptionLore
    ) {
        Inventory inv = Bukkit.createInventory(player, 9, title);

        // filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName("");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, filler);
        }

        // CONFIRMAR
        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a✔ Confirmar");
        confirmMeta.setLore(descriptionLore);
        confirm.setItemMeta(confirmMeta);

        // CANCELAR
        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName("§c✖ Cancelar");
        cancel.setItemMeta(cancelMeta);

        inv.setItem(3, confirm);
        inv.setItem(5, cancel);

        player.openInventory(inv);
        uxService.playSoundFromConfig(player, "open_menu");
    }

    @org.bukkit.event.EventHandler
    public void onDeleteConfirmationClick(org.bukkit.event.inventory.InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        // Só tratar GUIs de confirmação do Kamikeys
        String title = event.getView().getTitle();
        if (title == null || !title.startsWith("Apagar keys")) {
            return;
        }


        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        var pending = plugin.getPendingActionManager().getPendingAction(playerId);
        if (pending == null) {
            player.closeInventory();
            return;
        }

        // CONFIRMAR
        if (clicked.getType() == Material.LIME_CONCRETE) {
            plugin.getPendingActionManager().confirmAction(playerId);

            executeDeleteAction(
                    player,
                    pending.getTarget(),
                    pending.getFilter()
            );

            player.closeInventory();
            uxService.playSoundFromConfig(player, "confirm_success");
            return;
        }

        // CANCELAR
        if (clicked.getType() == Material.RED_CONCRETE) {
            plugin.getPendingActionManager().cancelAction(playerId);
            player.closeInventory();
            uxService.playSoundFromConfig(player, "cancel");
        }
    }

    @org.bukkit.event.EventHandler
    public void onDeleteConfirmationClose(org.bukkit.event.inventory.InventoryCloseEvent event) {

        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        String title = event.getView().getTitle();
        if (title == null || !title.startsWith("Apagar keys")) return;

        if (plugin.getPendingActionManager().hasPendingAction(playerId)) {
            plugin.getPendingActionManager().cancelAction(playerId);
            uxService.playSoundFromConfig(player, "cancel");
        }

    }


    private void handleConfirmar(CommandSender sender) {
        if (!sender.hasPermission("kamikeys.admin.*")) {
            uxService.sendError(sender, "common.no_permission", MessageUtils.createPlaceholders());
            uxService.playSoundFromConfig(sender, "error");
            return;
        }

        UUID playerId = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        if (playerId == null) {
            uxService.sendError(sender, "common.error", MessageUtils.createPlaceholders("details", "Apenas jogadores podem confirmar ações destrutivas."));
            return;
        }

        com.kamiplugins.kamikeys.utils.PendingActionManager.PendingAction pendingAction = plugin.getPendingActionManager().getPendingAction(playerId);
        if (pendingAction == null) {
            // Nenhuma ação pendente
            String prefixPlugin = uxService.getMessage("general.prefix");
            Map<String, String> placeholders = MessageUtils.createPlaceholders("prefixplugin", prefixPlugin);
            String message = MessageUtils.applyPlaceholders(uxService.getMessage("confirm.none"), placeholders);
            uxService.playSoundFromConfig(sender, "error");
            String messageWithColors = MessageUtils.applyColor(message);
            sender.sendMessage(messageWithColors);
            return;
        }

        // Confirmar ação
        String actionType = pendingAction.getActionType();
        String target = pendingAction.getTarget();
        String filter = pendingAction.getFilter();

        if ("DELETE_KEYS".equals(actionType)) {
            // Confirmar ação pendente
            plugin.getPendingActionManager().confirmAction(playerId);

            // Enviar mensagem de confirmação
            String prefixPlugin = uxService.getMessage("general.prefix");
            Map<String, String> placeholders = MessageUtils.createPlaceholders("prefixplugin", prefixPlugin);
            String message = MessageUtils.applyPlaceholders(uxService.getMessage("confirm.done"), placeholders);
            String messageWithColors = MessageUtils.applyColor(message);
            sender.sendMessage(messageWithColors);

            // Tocar som de confirmação
            uxService.playSoundFromConfig(sender, "confirm_success");

            // Simular a execução da ação (será substituído pela lógica real depois)
            executeDeleteAction(sender, target, filter);
        }
    }

    private void executeDeleteAction(CommandSender sender, String target, String filter) {
        String actor = sender.getName();

        // ================= ORIGEM =================
        if ("ORIGEM".equalsIgnoreCase(target)) {

            if (filter == null || (!"interna".equalsIgnoreCase(filter) && !"venda".equalsIgnoreCase(filter))) {
                sender.sendMessage("§cOrigem inválida. Use: interna ou venda.");
                return;
            }

            KeyOrigin origin = "interna".equalsIgnoreCase(filter) ? INTERNA : KeyOrigin.VENDA;

            // Para VENDA, normalmente as keys ficam com state VENDA.
            // Para INTERNA, normalmente fica ATIVA.
            List<Key> keys = plugin.getKeyService()
                    .getKeysByOrigin(origin)
                    .stream()
                    .filter(k -> {
                        if (k == null) return false;
                        if (origin == KeyOrigin.VENDA) {
                            return k.getState() == KeyState.VENDA || k.getState() == KeyState.ATIVA;
                        }
                        return k.getState() == KeyState.ATIVA;
                    })
                    .toList();

            if (keys.isEmpty()) {
                sender.sendMessage("§e⚠ Nenhuma key encontrada para a origem §f" + filter.toUpperCase());
                return;
            }

            KeyBatchDeleteResult result = plugin.getKeyService().deleteBatch(
                    keys,
                    actor,
                    "/kamikeys apagar origem " + filter
            );

            sender.sendMessage(" ");
            sender.sendMessage("§a✔ §cTodas as keys de origem §bINTERNA §cforam processadas. " + " §7| Apagadas: §f" + result.getApagadas() + " §7| Ignoradas: §f" + result.getIgnoradasPorEstado());
            sender.sendMessage(" ");

            // remover pending action após execução bem-sucedida
            if (sender instanceof Player) {
                plugin.getPendingActionManager().cancelAction(
                        ((Player) sender).getUniqueId()
                );
            }

            return;
        }

        // ================= PLAYER =================
        if ("PLAYER".equalsIgnoreCase(target)) {
            if (filter == null) {
                sender.sendMessage("§cPlayer inválido.");
                return;
            }

            KeyBatchDeleteResult result = plugin.getKeyService().deleteKeysByPlayer(
                    filter,
                    actor,
                    "/kamikeys apagar player " + filter
            );

            sender.sendMessage(" ");
            sender.sendMessage("§a✔ §cTodas as keys do §e" + filter + "§c foram processadas." + " §7| Apagadas: §f" + result.getApagadas() + " §7| Ignoradas: §f" + result.getIgnoradasPorEstado());
            sender.sendMessage(" ");

            // remover pending action após execução bem-sucedida
            if (sender instanceof Player) {
                plugin.getPendingActionManager().cancelAction(
                        ((Player) sender).getUniqueId()
                );
            }

            return;
        }

        // ================= PLAYER ALL =================
        if ("PLAYER_ALL".equalsIgnoreCase(target)) {

            KeyBatchDeleteResult result = plugin.getKeyService().deleteKeysByPlayer(
                    null,
                    actor,
                    "/kamikeys apagar player all"
            );

            sender.sendMessage(" ");
            sender.sendMessage("§a✔ §cAs keys de §4TODOS §cos players foram processadas." + " §7| Apagadas: §f" + result.getApagadas() + " §7| Ignoradas: §f" + result.getIgnoradasPorEstado());
            sender.sendMessage(" ");

            //Title para o player confirmando a ação de apagar todas as keys dos players
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.sendTitle(
                        "§c§lTODAS AS KEYS APAGADAS",
                        "§7Confira o chat para detalhes",
                        10, 70, 20
                );
            }

            // remover pending action após execução bem-sucedida
            if (sender instanceof Player) {
                plugin.getPendingActionManager().cancelAction(
                        ((Player) sender).getUniqueId()
                );
            }

            return;
        }

        // ================= TIPO =================
        if ("TIPO".equalsIgnoreCase(target)) {

            String tipo = filter;
            if (tipo == null || tipo.isBlank()) {
                sender.sendMessage("§cTipo inválido.");
                return;
            }

            // IMPORTANTÍSSIMO: aqui é getAllKeys() e filtra pelo getType()
            List<Key> keys = plugin.getKeyService()
                    .getAllKeys()
                    .stream()
                    .filter(k -> k != null
                            && k.getType() != null
                            && tipo.equalsIgnoreCase(k.getTypeKey())
                            && (k.getState() == KeyState.ATIVA || k.getState() == KeyState.VENDA)
                    )
                    .toList();

            if (keys.isEmpty()) {
                sender.sendMessage("§e⚠ Nenhuma key encontrada para o tipo §f" + tipo);
                return;
            }

            KeyBatchDeleteResult result = plugin.getKeyService().deleteBatch(
                    keys,
                    actor,
                    "/kamikeys apagar tipo " + tipo
            );

            sender.sendMessage(" ");
            sender.sendMessage("§a✔ §cAs keys do tipo §l" + tipo + "§c foram processadas." + " §7| Apagadas: §f" + result.getApagadas() + " §7| Ignoradas: §f" + result.getIgnoradasPorEstado());
            sender.sendMessage(" ");

            // remover pending action após execução bem-sucedida
            if (sender instanceof Player) {
                plugin.getPendingActionManager().cancelAction(
                        ((Player) sender).getUniqueId()
                );
            }

            return;
        }

        // ================= TUDO =================
        if ("TUDO".equalsIgnoreCase(target)) {

            List<Key> keys = plugin.getKeyService()
                    .getAllKeys()
                    .stream()
                    .filter(k -> k != null
                            && (k.getState() == KeyState.ATIVA || k.getState() == KeyState.VENDA)
                    )
                    .toList();

            if (keys.isEmpty()) {
                sender.sendMessage("§e⚠ Nenhuma key encontrada para apagar.");
                return;
            }

            KeyBatchDeleteResult result = plugin.getKeyService().deleteBatch(
                    keys,
                    actor,
                    "/kamikeys apagar tudo"
            );

            sender.sendMessage(" ");
            sender.sendMessage("§a✔ §4TODAS as keys do servidor foram processadas" + " §7| Apagadas: §f" + result.getApagadas() + " §7| Ignoradas: §f" + result.getIgnoradasPorEstado());
            sender.sendMessage(" ");

            //Title para o player confirmando a ação de apagar tudo
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.sendTitle(
                        "§c§lTODAS AS KEYS APAGADAS",
                        "§7Confira o chat para detalhes",
                        10, 70, 20
                );
            }

            // remover pending action após execução bem-sucedida
            if (sender instanceof Player) {
                plugin.getPendingActionManager().cancelAction(
                        ((Player) sender).getUniqueId()
                );
            }

            return;
        }


        // ================= FALLBACK =================
        sender.sendMessage("§cEste modo ainda não está disponível.");
    }





    private void handleBackup(CommandSender sender) {

        if (!sender.hasPermission("kamikeys.admin.*")) {
            sender.sendMessage("§cSem permissão.");
            return;
        }

        try {

            File dataFolder = plugin.getDataFolder();
            File backupFolder = new File(dataFolder, "backups");

            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }

            String date = new SimpleDateFormat("dd_MM_yyyy")
                    .format(new Date());

            File zipFile = new File(backupFolder, "backup_" + date + ".zip");

            try (java.util.zip.ZipOutputStream zos =
                         new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipFile))) {

                addFileToZip(new File(dataFolder, "keys.yml"), zos);
                addFileToZip(new File(dataFolder, "vouchers.yml"), zos);

                File logsFolder = new File(dataFolder, "logs");
                if (logsFolder.exists()) {
                    for (File monthFolder : Objects.requireNonNull(logsFolder.listFiles())) {
                        if (monthFolder.isDirectory()) {
                            for (File logFile : Objects.requireNonNull(monthFolder.listFiles())) {
                                addFileToZip(logFile, zos);
                            }
                        }
                    }
                }
            }

            sender.sendMessage(" ");
            sender.sendMessage("§aBackup completo criado com sucesso!");
            sender.sendMessage("§7Arquivo: §f" + zipFile.getName());
            sender.sendMessage(" ");

            uxService.playSoundFromConfig(sender,"backup");

            String actor = sender.getName();
            Player player = sender instanceof Player ? (Player) sender : null;
            String ip = player != null && player.getAddress() != null
                    ? player.getAddress().getAddress().getHostAddress()
                    : "CONSOLE";

            auditLogger.logKeyEvent(
                    AuditLogger.AuditAction.BACKUP,
                    "NULL",
                    "NULL",
                    "NULL",
                    null,
                    null,
                    AuditLogger.AuditActor.player(actor),
                    ip,
                    AuditLogger.AuditSource.command("/kamikeys backup"),
                    "Backup manual executado"
            );


        } catch (Exception e) {
            sender.sendMessage("§cErro ao criar backup.");
            e.printStackTrace();
        }
    }

    private void addFileToZip(File file, java.util.zip.ZipOutputStream zos) throws Exception {

        if (!file.exists()) return;

        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {

            java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;

            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
        }
    }


    private void handleReload(CommandSender sender) {
        // Garantir que o dataFolder exista
        java.io.File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Garantir config.yml
        java.io.File configFile = new java.io.File(dataFolder, "config.yml");
        boolean configCreated = false;
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            configCreated = true;
        }

        // Garantir messages.yml
        java.io.File messagesFile = new java.io.File(dataFolder, "messages.yml");
        boolean messagesCreated = false;
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
            messagesCreated = true;
        }

        // Recarregar configs
        plugin.reloadConfig();
        plugin.getConfigManager().reload();

        // Montar status dos arquivos
        StringBuilder status = new StringBuilder();
        status.append("config.yml=");
        status.append(configCreated ? "CRIADO" : "OK");
        status.append(", messages.yml=");
        status.append(messagesCreated ? "CRIADO" : "OK");

        // Enviar feedback
        uxService.sendAdmin(sender, "admin.reload_done", MessageUtils.createPlaceholders("files_status", status.toString()));

        // Tocar som de sucesso configurado para reload
        uxService.playSoundFromConfig(sender, "reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("kamikeys.admin.staff")) {
            return completions;
        }

        if (args.length == 1) {
            completions.add("help");
            completions.add("list");
            completions.add("gerar");
            completions.add("dar");
            completions.add("apagar");
            completions.add("backup");
            completions.add("reload");
        } else if (args.length == 2) {
            String subcommand = args[0];
            switch (subcommand) {
                case "gerar":
                    completions.add("venda");
                    completions.add("interna");
                    break;
                case "apagar":
                    completions.add("origem");
                    completions.add("tipo");
                    completions.add("player");
                    completions.add("tudo");
                    break;
                case "dar":
                    // Sugerir jogadores online
                    for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    break;
            }
        } else if (args.length == 3) {
            String subcommand = args[0];
            switch (subcommand) {
                case "apagar":
                    String mode = args[1];
                    switch (mode) {
                        case "origem":
                            completions.add("venda");
                            completions.add("interna");
                            break;
                        case "tipo":
                            // Sugerir tipos existentes na config
                            FileConfiguration config = plugin.getConfig();
                            for (String type : config.getConfigurationSection("Types").getKeys(false)) {
                                completions.add(type);
                            }
                            break;
                        case "player":
                            // Sugerir jogadores online
                            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                                completions.add(player.getName());
                            }
                            break;
                    }
                    break;
                case "gerar":
                    // Sugerir tipos existentes na config
                    FileConfiguration config = plugin.getConfig();
                    for (String type : config.getConfigurationSection("Types").getKeys(false)) {
                        completions.add(type);
                    }
                    break;
                case "dar":
                    // Sugerir tipos existentes na config
                    FileConfiguration config2 = plugin.getConfig();
                    for (String type : config2.getConfigurationSection("Types").getKeys(false)) {
                        completions.add(type);
                    }
                    break;
            }
        } else if (args.length == 4) {
            String subcommand = args[0];
            if ("gerar".equals(subcommand)) {
                // Sugerir quantidades comuns
                completions.add("1");
                completions.add("5");
                completions.add("10");
                completions.add("25");
                completions.add("50");
                completions.add("100");
            }
        }

        return completions;
    }
}
