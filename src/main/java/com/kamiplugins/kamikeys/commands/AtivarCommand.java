package com.kamiplugins.kamikeys.commands;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AtivarCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public AtivarCommand(Main plugin) {
        this.plugin = plugin;
    }
    public void executeActivation(Player player, String key) {
        // Chama o método principal de ativação
        activateKey(player, key, player.getName());
    }

    public void activateKey(Player player, String key, String playerName) {
        FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();

        if (!keysConfig.contains("keys." + key)) {
            player.sendMessage(ColorUtils.translate("&c❌ Key inválida ou já usada!"));
            return;
        }

        String tipo = keysConfig.getString("keys." + key + ".tipo");
        if (tipo == null) {
            player.sendMessage(ColorUtils.translate("&cErro ao ler tipo da key!"));
            return;
        }

        // Executa comandos do tipo
        List<String> commands = plugin.getConfig().getStringList("Types." + tipo + ".Commands");
        for (String cmd : commands) {
            cmd = cmd.replace("{player}", playerName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        // Remove a key
        keysConfig.set("keys." + key, null);
        plugin.getConfigManager().saveKeys();

        // Mensagem de sucesso
        player.sendMessage(ColorUtils.translate("&a✅ Key ativada com sucesso!"));

        // Log
        String details = "Tipo: " + tipo + " | Por: " + playerName; // ← Agora funciona
        logActivation(key, "ATIVADA", details);

        // Som (se habilitado)
        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
            String sound = plugin.getConfig().getString("Sounds.Player.KeyActivated", "entity.player.levelup");
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }

        // Atualiza a GUI do jogador (se estiver aberta)
        if (player.isOnline()) {
            if (plugin.getPlayerKeysGUIs().containsKey(player.getUniqueId())) {
                plugin.getPlayerKeysGUIs().get(player.getUniqueId()).refresh();
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

    private String findConfigKeyIgnoreCase(String typeName) {
        if (plugin.getConfig().getConfigurationSection("Types") == null) {
            return null;
        }
        for (String key : plugin.getConfig().getConfigurationSection("Types").getKeys(false)) {
            if (key.equalsIgnoreCase(typeName)) {
                return key;
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem ativar chaves.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage(ColorUtils.translate("&cUso incorreto! Use: /ativar <key>"));
            return true;
        }

        // Normaliza a key de acordo com a configuração (case-sensitive ou não)
        String keyCode = plugin.getKeyManager().normalizeKey(args[0]);
        FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
        String path = "keys." + keyCode;

        if (!keysConfig.contains(path)) {
            player.sendMessage(ColorUtils.translate("&cChave inválida ou já utilizada!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        // Verifica se é key exclusiva
        String exclusivoNome = keysConfig.getString(path + ".exclusivo_para.nome");
        String exclusivoUUID = keysConfig.getString(path + ".exclusivo_para.uuid");

        if (exclusivoNome != null && exclusivoUUID != null) {
            // Compara UUID (mais seguro que nome)
            if (!player.getUniqueId().toString().equals(exclusivoUUID)) {
                player.sendMessage(ColorUtils.translate("&cEsta chave é exclusiva para outro jogador!"));
                return true;
            }
        }

        String tipoSalvo = keysConfig.getString(path + ".tipo");
        String configKeyName = findConfigKeyIgnoreCase(tipoSalvo);

        if (configKeyName == null) {
            player.sendMessage(ColorUtils.translate("&cErro: O tipo de chave '" + tipoSalvo + "' não está configurado!"));
            return true;
        }

        List<String> comandos = plugin.getConfig().getStringList("Types." + configKeyName + ".Commands");
        if (comandos == null || comandos.isEmpty()) {
            player.sendMessage(ColorUtils.translate("&cErro: Esta chave não tem recompensas configuradas."));
            return true;
        }

        // Executar comandos
        for (String cmd : comandos) {
            String finalCmd = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }

        // --- LÊ OS DADOS ANTES DE APAGAR ---
        tipoSalvo = keysConfig.getString(path + ".tipo", "desconhecido"); // ← sem "String"
        String origem = keysConfig.getString(path + ".origem", "desconhecida");
        String donoOriginal = null;
        if ("exclusiva".equals(origem)) {
            donoOriginal = keysConfig.getString(path + ".exclusivo_para.nome", "???");
        }

// --- APAGA A KEY ---
        keysConfig.set(path, null);
        plugin.getConfigManager().saveKeys();

// --- LOG DE ATIVAÇÃO ---
        String logAction = "ATIVADA";
        if ("exclusiva".equals(origem)) {
            logAction = "ATIVADA_EXCLUSIVA";
        }

        String logDetails = "Jogador: " + player.getName() + " | Tipo: " + tipoSalvo + " | Origem: " + origem;
        if (donoOriginal != null) {
            logDetails += " | Dono original: " + donoOriginal;
        }

        logActivation(keyCode, logAction, logDetails);
        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true)) {
            // "player" já existe nesse escopo (é o jogador que ativou)
            if (player instanceof Player) {
                String sound = plugin.getConfig().getString("Sounds.Player.KeyActivated", "entity.player.levelup");
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        }

        // Feedback com mensagem configurada
        String successMessage = plugin.getConfig().getString("Types." + configKeyName + ".SuccessMessage", "&aChave ativada com sucesso!");
        player.sendMessage(ColorUtils.translate(successMessage));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        // Log no console
        plugin.getLogger().info("LOG: " + player.getName() + " resgatou a key " + keyCode + " (Tipo: " + tipoSalvo + ")");

        return true;
    }

    private void logActivation(String key, String action, String details) {
        try (PrintWriter out = new PrintWriter(new FileWriter(plugin.getConfigManager().getLogsFile(), true))) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            out.println("[" + now.format(formatter) + "] [" + action + "] Key: " + key);
            out.println("| " + details);
            out.println();
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao escrever no log: " + e.getMessage());
        }
    }
}