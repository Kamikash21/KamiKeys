package com.kamiplugins.kamikeys.commands;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.managers.AuditLogger;
import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.models.enums.KeyState;
import com.kamiplugins.kamikeys.services.ActivationCooldownService;
import com.kamiplugins.kamikeys.services.KeyService;
import com.kamiplugins.kamikeys.services.ValidationService;
import com.kamiplugins.kamikeys.utils.MessageUtils;
import com.kamiplugins.kamikeys.utils.UxService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AtivarCommand implements CommandExecutor {
    private final Main plugin;
    private final KeyService keyService;
    private final ValidationService validationService;
    private final UxService uxService;
    private final ActivationCooldownService cooldownService; // Referência injetada
    private final AuditLogger auditLogger; // Referência para o audit logger

    public AtivarCommand(Main plugin, KeyService keyService, ValidationService validationService, ActivationCooldownService cooldownService) {
        this.plugin = plugin;
        this.keyService = keyService;
        this.validationService = validationService;
        this.uxService = plugin.getConfigManager().getUxService();
        this.cooldownService = cooldownService; // Injetar o serviço compartilhado
        this.auditLogger = plugin.getAuditLogger(); // Obter referência ao audit logger
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kamikeys.player")) {
            uxService.sendError(sender, "common.no_permission", MessageUtils.createPlaceholders());
            uxService.playSoundFromConfig(sender, "error");
            return true;
        }

        if (!(sender instanceof Player)) {
            uxService.sendError(sender, "common.only_player", MessageUtils.createPlaceholders());
            uxService.playSoundFromConfig(sender, "error");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            // Apenas mensagem de uso, sem title nem actionbar
            String prefixPlugin = uxService.getMessage("general.prefix");
            String usageMessage = uxService.getMessage("common.usage");
            String processedMessage = MessageUtils.applyPlaceholders(usageMessage, MessageUtils.createPlaceholders(
                    "usage", "/ativar <key>",
                    "prefixplugin", prefixPlugin
            ));
            String finalMessage = prefixPlugin + " " + processedMessage;
            player.sendMessage(MessageUtils.applyColor(finalMessage));
            uxService.playSoundFromConfig(player, "error");
            return true;
        }

        String keyCode = args[0];

        // Verificar cooldown usando o serviço compartilhado
        int cooldownSeconds = plugin.getConfig().getInt("Security.ActivationCooldownSeconds", 3);
        if (cooldownSeconds > 0) {
            if (cooldownService.isInCooldown(player)) {
                long remainingSeconds = cooldownService.getRemainingSeconds(player);

                // Obter mensagens usando o UxService existente
                // As chaves devem estar presentes em messages.yml

                // Mensagem principal
                String cooldownMessage = uxService.getMessage("activation.cooldown.message");

                Map<String, String> placeholders = MessageUtils.createPlaceholders(
                        "time", String.valueOf(remainingSeconds),
                        "prefixplugin", uxService.getMessage("general.prefix")
                );

                String processedMessage = MessageUtils.applyPlaceholders(cooldownMessage, placeholders);
                player.sendMessage(MessageUtils.applyColor(processedMessage));

                // Action bar específico para cooldown
                String actionBarMessage = uxService.getMessage("activation.cooldown.actionbar");
                String processedActionBar = MessageUtils.applyPlaceholders(actionBarMessage, placeholders);
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                                MessageUtils.applyColor(processedActionBar)
                        )
                );

                // Title específico para cooldown
                String cooldownTitle = uxService.getMessage("activation.cooldown.title");
                String cooldownSubtitle = uxService.getMessage("activation.cooldown.subtitle");
                String processedTitle = MessageUtils.applyPlaceholders(MessageUtils.applyColor(cooldownTitle), placeholders);
                String processedSubtitle = MessageUtils.applyPlaceholders(MessageUtils.applyColor(cooldownSubtitle), placeholders);
                player.sendTitle(processedTitle, processedSubtitle, 10, 70, 20);

                // Tocar som de erro
                uxService.playSoundFromConfig(player, "error");

                return true;
            }
        }

        // Buscar key
        Key key = keyService.findByCode(keyCode).orElse(null);
        if (key == null) {
            // Key não encontrada
            String prefixPlugin = uxService.getMessage("general.prefix");
            String errorMessage = uxService.getMessage("player.key_not_found");
            String processedMessage = MessageUtils.applyPlaceholders(errorMessage, MessageUtils.createPlaceholders(
                    "prefixplugin", prefixPlugin,
                    "key", keyCode
            ));
            String finalMessage = prefixPlugin + " " + processedMessage;
            player.sendMessage(MessageUtils.applyColor(finalMessage));

            // Action bar específico para erro
            String errorActionBar = uxService.getMessage("activation.error_key.actionbar");
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                            MessageUtils.applyColor(errorActionBar)
                    )
            );

            // Title específico para erro
            String errorTitle = uxService.getMessage("activation.error_key.title");
            String errorSubtitle = uxService.getMessage("activation.error_key.subtitle");
            player.sendTitle(
                    MessageUtils.applyColor(errorTitle),
                    MessageUtils.applyColor(errorSubtitle),
                    10, 70, 20
            );

            uxService.playSoundFromConfig(player, "error");
            return true;
        }

        // Validar estado
        if (key.getState() != KeyState.ATIVA && key.getState() != KeyState.VENDA) {
            // Key não ativa
            String prefixPlugin = uxService.getMessage("general.prefix");
            String errorMessage = uxService.getMessage("player.key_not_active");
            String processedMessage = MessageUtils.applyPlaceholders(errorMessage, MessageUtils.createPlaceholders(
                    "prefixplugin", prefixPlugin,
                    "key", keyCode
            ));
            String finalMessage = prefixPlugin + " " + processedMessage;
            player.sendMessage(MessageUtils.applyColor(finalMessage));

            // Action bar específico para erro
            String errorActionBar = uxService.getMessage("activation.error_key_used.actionbar");
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                            MessageUtils.applyColor(errorActionBar)
                    )
            );

            // Title específico para erro
            String errorTitle = uxService.getMessage("activation.error_key_used.title");
            String errorSubtitle = uxService.getMessage("activation.error_key_used.subtitle");
            player.sendTitle(
                    MessageUtils.applyColor(errorTitle),
                    MessageUtils.applyColor(errorSubtitle),
                    10, 70, 20
            );

            uxService.playSoundFromConfig(player, "error");
            return true;
        }

        // Validar exclusividade
        if (key.getExclusiveToName() != null && !key.getExclusiveToName().equalsIgnoreCase(player.getName())) {
            // Key não é do jogador
            String errorMessage = uxService.getMessage("player.key_not_yours");
            player.sendMessage(MessageUtils.applyColor(errorMessage));

            // Action bar específico para erro
            String errorActionBar = uxService.getMessage("effects.actionbar.error");
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                            MessageUtils.applyColor(errorActionBar)
                    )
            );

            // Title específico para erro
            String errorTitle = uxService.getMessage("effects.title.error.title");
            String errorSubtitle = uxService.getMessage("effects.title.error.subtitle");
            player.sendTitle(
                    MessageUtils.applyColor(errorTitle),
                    MessageUtils.applyColor(errorSubtitle),
                    10, 70, 20
            );

            uxService.playSoundFromConfig(player, "error");
            return true;
        }

        // Validar permissões de tipo (se aplicável)
        String typeKey = key.getTypeKey();
        if (!validationService.canPlayerUseKey(player, typeKey)) {
            // Sem permissão para usar a key
            String errorMessage = uxService.getMessage("player.key_no_permission");
            player.sendMessage(MessageUtils.applyColor(errorMessage));

            // Action bar específico para erro
            String errorActionBar = uxService.getMessage("effects.actionbar.error");
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                            MessageUtils.applyColor(errorActionBar)
                    )
            );

            // Title específico para erro
            String errorTitle = uxService.getMessage("effects.title.error.title");
            String errorSubtitle = uxService.getMessage("effects.title.error.subtitle");
            player.sendTitle(
                    MessageUtils.applyColor(errorTitle),
                    MessageUtils.applyColor(errorSubtitle),
                    10, 70, 20
            );

            uxService.playSoundFromConfig(player, "error");
            return true;
        }

        // Executar comandos da key
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("Types." + typeKey + ".Commands");

        for (String commandStr : commands) {
            String finalCommand = commandStr.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        }

        // Registrar ativação no audit logger
        // Obter nick do jogador (tenta displayName como "nick", senão usa o nome original)
        String playerNick = (player.getDisplayName() != null && !player.getDisplayName().isEmpty()) ? player.getDisplayName() : player.getName();
        String playerIp = "SYSTEM";
        if (player.getAddress() != null && player.getAddress().getAddress() != null) {
            try {
                playerIp = player.getAddress().getAddress().getHostAddress();
            } catch (Exception ignored) {
                // fallback para SYSTEM se algo der errado
                playerIp = "SYSTEM";
            }
        }

        auditLogger.logKeyEvent(
                AuditLogger.AuditAction.ATIVADA,
                key.getCode(),
                key.getTypeKey(),
                key.getOrigin().name(),
                KeyState.ATIVA,
                KeyState.USADA,
                AuditLogger.AuditActor.player(playerNick + " | UUID : " + player.getUniqueId().toString()),
                playerIp,
                AuditLogger.AuditSource.command("/ativar"),
                "Ativação bem-sucedida da key"
        );

        // Marcar key como USADA
        key.setState(KeyState.USADA);
        key.setActivatedBy(player.getName());
        key.setActivatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        key.setActivatedByUuid(player.getUniqueId().toString());

        keyService.deleteKey(keyCode);

        // Registrar cooldown NOVAMENTE usando o serviço compartilhado (APÓS sucesso)
        cooldownService.register(player);

        // Mensagem de sucesso
        String reward = config.getString("Types." + typeKey + ".Recompensa", "Recompensa desconhecida");
        String prefixPlugin = uxService.getMessage("general.prefix");
        Map<String, String> placeholders = MessageUtils.createPlaceholders(
                "prefixplugin", prefixPlugin,
                "reward", reward
        );
        String successMessage = MessageUtils.applyPlaceholders(uxService.getMessage("player.activation_success_detailed"), placeholders);
        String successMessageWithColors = MessageUtils.applyColor(successMessage);
        player.sendMessage(successMessageWithColors);

        // Tocar som de sucesso
        uxService.playSoundFromConfig(player, "success");
        uxService.playParticcle(player, "activation_success");
        uxService.playFirework(player);

        // Enviar title de sucesso específico
        String successTitle = uxService.getMessage("activation.success.title");
        String successSubtitle = uxService.getMessage("activation.success.subtitle");

        Map<String, String> titlePlaceholders = MessageUtils.createPlaceholders(
                "reward", reward
        );

        String processedTitle = MessageUtils.applyPlaceholders(MessageUtils.applyColor(successTitle), titlePlaceholders);
        String processedSubtitle = MessageUtils.applyPlaceholders(MessageUtils.applyColor(successSubtitle), titlePlaceholders);

        player.sendTitle(processedTitle, processedSubtitle, 10, 70, 20);

        // Enviar actionbar de sucesso específico
        String successActionBar = uxService.getMessage("activation.success.actionbar");
        String processedSuccessActionBar = MessageUtils.applyPlaceholders(MessageUtils.applyColor(successActionBar), placeholders);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(processedSuccessActionBar));

        // Gerar partícula de sucesso
        if (plugin.getConfig().getBoolean("effects.enabled", true) &&
                plugin.getConfig().getBoolean("effects.particles.enabled", false)) {

            String particleType = plugin.getConfig().getString("effects.particles.success.type", "VILLAGER_HAPPY");
            int count = plugin.getConfig().getInt("effects.particles.success.count", 10);

            try {
                org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleType);
                player.getWorld().spawnParticle(particle, player.getLocation(), count);
            } catch (Exception e) {
                // Silently fail if particle is invalid
            }
        }

        return true;
    }
}
