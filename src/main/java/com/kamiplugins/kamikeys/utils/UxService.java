package com.kamiplugins.kamikeys.utils;

import com.kamiplugins.kamikeys.Main;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UxService {

    private final Main plugin;
    private FileConfiguration messagesConfig;
    private final File messagesFile;

    // Rate limiter para partículas
    private final Map<UUID, Long> lastParticleTime = new ConcurrentHashMap<>();

    public UxService(Main plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        loadMessages();
    }

    private void loadMessages() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadMessages() {
        loadMessages();
    }

    // Tornar público para acesso externo
    public String getMessage(String path) {
        return messagesConfig.getString(path, "MENSAGEM_NAO_ENCONTRADA: " + path);
    }

    // Método para tocar som a partir do config
    public void playSoundFromConfig(CommandSender sender, String key) {
        if (!(sender instanceof Player)) {
            return; // Apenas para players
        }

        Player player = (Player) sender;

        // Verificar se o som específico está habilitado
        String enabledPath = "Feedback.Sounds." + key + ".Enabled";
        if (!plugin.getConfig().getBoolean(enabledPath, true)) {
            return;
        }

        // Obter configurações do som
        String soundName = plugin.getConfig().getString("Feedback.Sounds." + key + ".Sound", "ENTITY_PLAYER_LEVELUP");
        float volume = (float) plugin.getConfig().getDouble("Feedback.Sounds." + key + ".Volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("Feedback.Sounds." + key + ".Pitch", 1.0);

        // Validar e tocar som
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Som inválido, apenas ignora (fail safe)
            plugin.getLogger().warning("Som inválido configurado em Feedback.Sounds." + key + ".Sound: " + soundName);
        }
    }

    private void sendRawMessage(CommandSender recipient, String message) {
        if (recipient instanceof Player) {
            Player player = (Player) recipient;
            // Multi-linha
            if (message.contains("\n")) {
                String[] lines = message.split("\n");
                for (String line : lines) {
                    player.sendMessage(line);
                }
            } else {
                player.sendMessage(message);
            }
        } else {
            recipient.sendMessage(ChatColor.stripColor(message));
        }
    }

    private void playSound(Player player, String category) {
        if (!plugin.getConfig().getBoolean("sounds.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("sounds." + category + ".enabled", true)) return;

        String soundName = plugin.getConfig().getString("sounds." + category + ".sound", "ENTITY_PLAYER_LEVELUP");
        float volume = (float) plugin.getConfig().getDouble("sounds." + category + ".volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("sounds." + category + ".pitch", 1.0);

        try {
            player.playSound(player.getLocation(), soundName, volume, pitch);
        } catch (Exception e) {
            // Silently fail if sound is invalid
        }
    }

    private void sendTitle(Player player, String titlePath, String subtitlePath) {
        if (!plugin.getConfig().getBoolean("effects.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("effects.title.enabled", true)) return;

        String title = MessageUtils.applyColor(getMessage(titlePath));
        String subtitle = MessageUtils.applyColor(getMessage(subtitlePath));

        int fadeIn = plugin.getConfig().getInt("effects.title.fadeIn", 10);
        int stay = plugin.getConfig().getInt("effects.title.stay", 70);
        int fadeOut = plugin.getConfig().getInt("effects.title.fadeOut", 20);

        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    private void sendActionBar(Player player, String messageKey) {
        if (!plugin.getConfig().getBoolean("effects.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("effects.actionbar.enabled", true)) return;

        String message = MessageUtils.applyColor(getMessage(messageKey));
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }

    private void spawnParticle(Player player, String category) {
        if (!plugin.getConfig().getBoolean("effects.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("effects.particles.enabled", false)) return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastTime = lastParticleTime.getOrDefault(playerId, 0L);
        int maxPerSecond = plugin.getConfig().getInt("effects.particles.max_per_second", 10);
        long minInterval = 1000 / maxPerSecond; // Milissegundos por partícula

        if (currentTime - lastTime < minInterval) {
            // Rate limit excedido, não spawnar
            return;
        }

        lastParticleTime.put(playerId, currentTime);

        String particleType = plugin.getConfig().getString("effects.particles." + category + ".type", "VILLAGER_HAPPY");
        int count = plugin.getConfig().getInt("effects.particles." + category + ".count", 10);

        Location location = player.getLocation();
        try {
            Particle particle = Particle.valueOf(particleType);
            player.getWorld().spawnParticle(particle, location, count);
        } catch (Exception e) {
            // Silently fail if particle is invalid
        }
    }

    private String applyPrefix(String message, boolean isAdminOrError) {
        if (isAdminOrError) {
            String prefix = MessageUtils.applyColor(getMessage("general.prefix"));
            return prefix + message;
        }
        return message;
    }

    public void sendInfo(CommandSender recipient, String messageKey, Map<String, String> placeholders) {
        String message = getMessage(messageKey);
        message = MessageUtils.applyPlaceholders(message, placeholders);
        message = MessageUtils.applyColor(message);
        sendRawMessage(recipient, message);

        if (recipient instanceof Player) {
            Player player = (Player) recipient;
            sendActionBar(player, "effects.actionbar.success");
            playSound(player, "info");
            spawnParticle(player, "success");
        }
    }

    public void sendSuccess(CommandSender recipient, String messageKey, Map<String, String> placeholders) {
        String message = getMessage(messageKey);
        message = MessageUtils.applyPlaceholders(message, placeholders);
        message = MessageUtils.applyColor(message);
        sendRawMessage(recipient, message);

        if (recipient instanceof Player) {
            Player player = (Player) recipient;
            sendTitle(player, "effects.title.success.title", "effects.title.success.subtitle");
            sendActionBar(player, "effects.actionbar.success");
            playSound(player, "success");
            spawnParticle(player, "success");
        }
    }

    public void sendError(CommandSender recipient, String messageKey, Map<String, String> placeholders) {
        String message = getMessage(messageKey);
        message = MessageUtils.applyPlaceholders(message, placeholders);
        message = MessageUtils.applyColor(message);
        message = applyPrefix(message, true); // Always prefix errors
        sendRawMessage(recipient, message);

        if (recipient instanceof Player) {
            Player player = (Player) recipient;
            sendTitle(player, "effects.title.error.title", "effects.title.error.subtitle");
            sendActionBar(player, "effects.actionbar.error");
            playSound(player, "error");
            spawnParticle(player, "error");
        }
    }

    public void sendAdmin(CommandSender recipient, String messageKey, Map<String, String> placeholders) {
        String message = getMessage(messageKey);
        message = MessageUtils.applyPlaceholders(message, placeholders);
        message = MessageUtils.applyColor(message);
        message = applyPrefix(message, true); // Always prefix admin messages
        sendRawMessage(recipient, message);

        if (recipient instanceof Player) {
            Player player = (Player) recipient;
            playSound(player, "admin");
        }
    }

    public void sendConfirmPending(CommandSender recipient, String messageKey, Map<String, String> placeholders) {
        String message = getMessage(messageKey);
        message = MessageUtils.applyPlaceholders(message, placeholders);
        message = MessageUtils.applyColor(message);
        message = applyPrefix(message, true); // Prefix for destructive actions
        sendRawMessage(recipient, message);

        if (recipient instanceof Player) {
            Player player = (Player) recipient;
            sendTitle(player, "effects.title.confirm_pending.title", "effects.title.confirm_pending.subtitle");
            sendActionBar(player, "effects.actionbar.confirm_pending");
            playSound(player, "confirm_pending");
            spawnParticle(player, "confirm_pending");
        }
    }

    public void sendConfirmDone(CommandSender recipient, String messageKey, Map<String, String> placeholders) {
        String message = getMessage(messageKey);
        message = MessageUtils.applyPlaceholders(message, placeholders);
        message = MessageUtils.applyColor(message);
        message = applyPrefix(message, true); // Prefix for destructive actions
        sendRawMessage(recipient, message);

        if (recipient instanceof Player) {
            Player player = (Player) recipient;
            sendTitle(player, "effects.title.confirm_done.title", "effects.title.confirm_done.subtitle");
            sendActionBar(player, "effects.actionbar.success");
            playSound(player, "confirm_done"); // Changed from "success" to "confirm_done"
            spawnParticle(player, "confirm_done");
        }
    }

    public void sendCooldownWait(CommandSender recipient, int seconds) {
        Map<String, String> placeholders = MessageUtils.createPlaceholders("time", String.valueOf(seconds));
        sendInfo(recipient, "rate_limit.wait", placeholders);

        if (recipient instanceof Player) {
            Player player = (Player) recipient;
            sendActionBar(player, "effects.actionbar.cooldown");
        }
    }

    public void playParticcle(Player player,String key) {
        ConfigurationSection section = plugin.getConfig()
                .getConfigurationSection("effects.particles." + key);

        if (section == null || !section.getBoolean("enabled", false)) {
            return;
        }

        Particle particle;
        try {
            particle = Particle.valueOf(section.getString("type", "VILLAGER_HAPPY"));
        } catch (IllegalArgumentException e) {
            return; // Tipo de partícula inválido
        }

        int count = section.getInt("count", 10);
        double offsetX = section.getDouble("offset_x", 0.3);
        double offsetY = section.getDouble("offset_y", 1.0);
        double offsetZ = section.getDouble("offset_z", 0.3);
        double speed = section.getDouble("speed", 0.0);


        player.getWorld().spawnParticle(
                particle,
                player.getLocation().add(0, 1, 0),
                count,
                offsetX,
                offsetY,
                offsetZ,
                speed
        );
    }

    public void playFirework(Player player) {
        if (!plugin.getConfig().getBoolean("effects.firework.enabled", false)) {
            return;
        }

        Location loc = player.getLocation();

        Firework firework = (Firework) player.getWorld()
                .spawnEntity(loc, EntityType.FIREWORK_ROCKET);

        FireworkMeta meta = firework.getFireworkMeta();

        FireworkEffect.Builder builder = FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withFlicker()
                .withTrail();

        // Cores principais
        for (String colorName : plugin.getConfig().getStringList("effects.firework.colors")) {
            Color color = parseColor(colorName);
            if (color != null) {
                builder.withColor(color);
            }
        }

        // Cores de fade
        for (String colorName : plugin.getConfig().getStringList("effects.firework.fadeColors")) {
            Color color = parseColor(colorName);
            if (color != null) {
                builder.withFade(color);
            }
        }

        meta.addEffect(builder.build());
        meta.setPower(plugin.getConfig().getInt("effects.firework.power", 1));

        firework.setFireworkMeta(meta);
    }


    private Color parseColor(String name) {
        switch (name.toUpperCase()) {
            case "RED": return Color.RED;
            case "GREEN": return Color.GREEN;
            case "BLUE": return Color.BLUE;
            case "AQUA": return Color.AQUA;
            case "LIME": return Color.LIME;
            case "YELLOW": return Color.YELLOW;
            case "ORANGE": return Color.ORANGE;
            case "PURPLE": return Color.PURPLE;
            case "FUCHSIA": return Color.FUCHSIA;
            case "WHITE": return Color.WHITE;
            case "GRAY": return Color.GRAY;
            case "BLACK": return Color.BLACK;
            default: return null;
        }
    }
}