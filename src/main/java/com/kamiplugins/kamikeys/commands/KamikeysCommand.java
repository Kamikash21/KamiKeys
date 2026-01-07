package com.kamiplugins.kamikeys.commands;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.admin.AdminMainMenu;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class KamikeysCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public KamikeysCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("kamikeys.admin")) {
            sender.sendMessage(plugin.getConfig().getString("Messages.NoPermission", "&cSem permissão."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ColorUtils.translate("&e[KamiKeys] &aComandos disponíveis:"));
            sender.sendMessage(ColorUtils.translate("&7/kamikeys venda <tipo> <qtd> &8- Gerar keys para vendas externas"));
            sender.sendMessage(ColorUtils.translate("&7/kamikeys gerar <tipo> <qtd> &8- Gerar keys para eventos internos"));
            sender.sendMessage(ColorUtils.translate("&7/kamikeys dar <player> <tipo> &8- Dar key exclusiva a um jogador"));
            sender.sendMessage(ColorUtils.translate("&7/kamikeys apagar origem <venda|interna|exclusiva> &8- Apagar por origem"));
            sender.sendMessage(ColorUtils.translate("&7/kamikeys apagar tipo <tipo> &8- Apagar todas as keys de um tipo"));
            sender.sendMessage(ColorUtils.translate("&7/kamikeys apagar player <player> &8- Apagar keys exclusivas de um jogador"));
            sender.sendMessage(ColorUtils.translate("&7/kamikeys apagar tudo &8- Apagar TODAS as keys"));
            sender.sendMessage(ColorUtils.translate("&7/kamikeys list &8- Ver todas as keys (GUI)"));
            sender.sendMessage(ColorUtils.translate("&7/kamikeys reload &8- Recarregar configurações"));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload": {
                // Recarrega todos os arquivos e recria se não existirem
                plugin.getConfigManager().reloadAll();

                // Mensagem de sucesso
                sender.sendMessage(ColorUtils.translate("&aConfigurações e arquivos recarregados com sucesso!"));

                // Opcional: informar se arquivos foram recriados
                if (!plugin.getConfigManager().getKeysFile().exists()) {
                    sender.sendMessage(ColorUtils.translate("&eArquivo &fkeys.yml &enão encontrado, criando padrão..."));
                }
                if (!plugin.getConfigManager().getConfigFile().exists()) {
                    sender.sendMessage(ColorUtils.translate("&eArquivo &fconfig.yml &enão encontrado, criando padrão..."));
                }

                // ✅ Som de sucesso
                if (plugin.getConfig().getBoolean("Settings.EnableSounds", true) && sender instanceof Player) {
                    Player p = (Player) sender;
                    String sound = plugin.getConfig().getString("Sounds.Admin.Success", "block.note_block.chime");
                    p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                }

                return true;
            }

            case "venda": {
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /kamikeys venda <tipo> <quantidade>");
                    return true;
                }
                String tipo = args[1];
                int quantidade;
                try {
                    quantidade = Integer.parseInt(args[2]);
                    if (quantidade <= 0 || quantidade > 100) {
                        sender.sendMessage("§cQuantidade deve ser entre 1 e 100.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cQuantidade inválida.");
                    return true;
                }

                if (!plugin.getConfig().contains("Types." + tipo)) {
                    sender.sendMessage("§cTipo '" + tipo + "' não existe no config.yml.");
                    return true;
                }

                StringBuilder keysGeradas = new StringBuilder();
                for (int i = 0; i < quantidade; i++) {
                    String key = plugin.getKeyManager().generateKey(tipo);
                    if (key == null) {
                        sender.sendMessage("§cErro ao gerar key do tipo " + tipo);
                        continue;
                    }

                    // ✅ SALVA COM ORIGEM "venda"
                    boolean saved = plugin.getKeyManager().saveKey(key, tipo, "venda", null, sender.getName());
                    if (saved) {
                        keysGeradas.append(key).append("\n");
                    }
                }

                String[] keysArray = keysGeradas.toString().trim().split("\n");
                if (keysArray.length > 0 && !keysArray[0].isEmpty()) {
                    String tipoFormatado = tipo.substring(0, 1).toUpperCase() + (tipo.length() > 1 ? tipo.substring(1).toLowerCase() : "");
                    String prefixColor = plugin.getConfig().getString("Types." + tipo + ".PrefixColor", "&8");
                    String header = "&e[KamiKeys] &f" + keysArray.length + " &aChaves " + prefixColor + "[" + tipoFormatado + "] &ageradas para venda!";
                    sender.sendMessage(ColorUtils.translate(header));

                    for (String key : keysArray) {
                        if (sender instanceof Player) {
                            TextComponent component = new TextComponent(ColorUtils.translate("&e🔑 Key: &b" + key));
                            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, key));
                            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new ComponentBuilder("Clique para copiar").create()));
                            ((Player) sender).spigot().sendMessage(component);
                        } else {
                            sender.sendMessage(ColorUtils.translate("&b" + key));
                        }

                        // Log individual
                        String details = "Tipo: " + tipo + " | Origem: venda";
                        logKey(key, "GERADA", details, sender.getName());

                        // Som
                        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true) && sender instanceof Player) {
                            Player p = (Player) sender;
                            String sound = plugin.getConfig().getString("Sounds.Admin.KeyGenerated", "block.note_block.pling");
                            p.playSound(p.getLocation(), sound, 1.0f, 1.5f);
                        }
                    }
                } else {
                    sender.sendMessage("§cNenhuma chave foi gerada.");
                }
                return true;

            }

            case "gerar": {
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /kamikeys gerar <tipo> <quantidade>");
                    return true;
                }
                String tipo = args[1];
                int quantidade;
                try {
                    quantidade = Integer.parseInt(args[2]);
                    if (quantidade <= 0 || quantidade > 100) {
                        sender.sendMessage("§cQuantidade deve ser entre 1 e 100.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cQuantidade inválida.");
                    return true;
                }

                if (!plugin.getConfig().contains("Types." + tipo)) {
                    sender.sendMessage("§cTipo '" + tipo + "' não existe no config.yml.");
                    return true;
                }

                StringBuilder keysGeradas = new StringBuilder();
                for (int i = 0; i < quantidade; i++) {
                    String key = plugin.getKeyManager().generateKey(tipo);
                    if (key == null) {
                        sender.sendMessage("§cErro ao gerar key do tipo " + tipo);
                        continue;
                    }

                    // ✅ SALVA COM ORIGEM "interna"
                    boolean saved = plugin.getKeyManager().saveKey(key, tipo, "interna", null, sender.getName());
                    if (saved) {
                        keysGeradas.append(key).append("\n");
                    }
                }

                String[] keysArray = keysGeradas.toString().trim().split("\n");
                if (keysArray.length > 0 && !keysArray[0].isEmpty()) {
                    String tipoFormatado = tipo.substring(0, 1).toUpperCase() + (tipo.length() > 1 ? tipo.substring(1).toLowerCase() : "");
                    String prefixColor = plugin.getConfig().getString("Types." + tipo + ".PrefixColor", "&8");
                    String header = "&e[KamiKeys] &f" + keysArray.length + " &aChaves " + prefixColor + "[" + tipoFormatado + "] &ageradas para eventos!";
                    sender.sendMessage(ColorUtils.translate(header));

                    for (String key : keysArray) {
                        if (sender instanceof Player) {
                            TextComponent component = new TextComponent(ColorUtils.translate("&e🔑 Key: &b" + key));
                            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, key));
                            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new ComponentBuilder("Clique para copiar").create()));
                            ((Player) sender).spigot().sendMessage(component);
                        } else {
                            sender.sendMessage(ColorUtils.translate("&b" + key));
                        }

                        // Log individual
                        String details = "Tipo: " + tipo + " | Origem: interna";
                        logKey(key, "GERADA", details, sender.getName());

                        // Som
                        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true) && sender instanceof Player) {
                            Player p = (Player) sender;
                            String sound = plugin.getConfig().getString("Sounds.Admin.KeyGenerated", "block.note_block.pling");
                            p.playSound(p.getLocation(), sound, 1.0f, 1.5f);
                        }
                    }
                } else {
                    sender.sendMessage("§cNenhuma chave foi gerada.");
                }
                return true;

            }

            case "dar": {
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /kamikeys dar <player> <tipo>");
                    return true;
                }
                String playerNome = args[1];
                String tipo = args[2];

                // Validações
                if (!plugin.getKeyManager().isPlayerKnown(playerNome)) {
                    sender.sendMessage(ColorUtils.translate("&cErro: O jogador '&f" + playerNome + "&c' nunca entrou neste servidor!"));
                    return true;
                }
                if (!plugin.getConfig().contains("Types." + tipo)) {
                    sender.sendMessage("§cTipo '" + tipo + "' não existe no config.yml.");
                    return true;
                }

                // Gera e salva key exclusiva
                String key = plugin.getKeyManager().generateKey(tipo);
                if (key == null) {
                    sender.sendMessage("§cErro ao gerar key do tipo " + tipo);
                    return true;
                }
                String playerUUID = plugin.getKeyManager().getPlayerUUID(playerNome);
                boolean saved = plugin.getKeyManager().saveExclusiveKey(key, tipo, playerNome, playerUUID, sender.getName());
                if (!saved) {
                    sender.sendMessage("§cErro: não foi possível salvar a key.");
                    return true;
                }

                // Formata data/hora legível
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

                // Mensagem de sucesso
                String tipoFormatado = tipo.substring(0, 1).toUpperCase() + (tipo.length() > 1 ? tipo.substring(1).toLowerCase() : "");
                String prefixColor = plugin.getConfig().getString("Types." + tipo + ".PrefixColor", "&8");
                String header = "&e[KamiKeys] &aKey " + prefixColor + "[" + tipoFormatado + "] &agerada para &f" + playerNome + " &aem &f" + now.format(formatter) + "&a!";
                sender.sendMessage(ColorUtils.translate(header));

                // Envia key e UUID como mensagens clicáveis
                if (sender instanceof Player) {
                    Player player = (Player) sender;

                    // Linha 1: UUID (clicável)
                    TextComponent uuidLine = new TextComponent(ColorUtils.translate("&8👤 UUID: "));
                    TextComponent uuidValue = new TextComponent(ColorUtils.translate(playerUUID + " &7[Clique para copiar]"));
                    uuidValue.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, playerUUID));
                    uuidValue.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder("Clique para sugerir no chat").create()));
                    uuidLine.addExtra(uuidValue);
                    player.spigot().sendMessage(uuidLine);

                    // Linha 2: Key (clicável)
                    TextComponent keyLine = new TextComponent(ColorUtils.translate("&e🔑 Key: "));
                    TextComponent keyValue = new TextComponent(ColorUtils.translate("&b" + key + " &7[Clique para copiar]"));
                    keyValue.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, key));
                    keyValue.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder("Clique para sugerir no chat").create()));
                    keyLine.addExtra(keyValue);
                    player.spigot().sendMessage(keyLine);

                } else {
                    // Console
                    sender.sendMessage(ColorUtils.translate("&bKey: " + key));
                    sender.sendMessage(ColorUtils.translate("&7UUID: " + playerUUID));
                }

                // Log e som
                String details = "Tipo: " + tipo + " | Para: " + playerNome + " (UUID: " + playerUUID + ")";
                logKey(key, "GERADA_EXCLUSIVA", details, sender.getName());

                if (plugin.getConfig().getBoolean("Settings.EnableSounds", true) && sender instanceof Player) {
                    Player p = (Player) sender;
                    String sound = plugin.getConfig().getString("Sounds.Admin.KeyGenerated", "block.note_block.pling"); // ← CORREÇÃO
                    p.playSound(p.getLocation(), sound, 1.0f, 1.5f);
                }

                return true;
            }

            case "apagar": {
                if (args.length < 2) {
                    sender.sendMessage("§cUso:");
                    sender.sendMessage("§7/kamikeys apagar origem <venda|interna|exclusiva>");
                    sender.sendMessage("§7/kamikeys apagar tipo <tipo>");
                    sender.sendMessage("§7/kamikeys apagar player <player>");
                    sender.sendMessage("§7/kamikeys apagar tudo");
                    return true;
                }

                String alvo = args[1];
                FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();

                if ("origem".equals(alvo) && args.length >= 3) {
                    String origem = args[2].toLowerCase();
                    if (!"venda".equals(origem) && !"interna".equals(origem) && !"exclusiva".equals(origem)) {
                        sender.sendMessage("§cOrigem inválida. Use: venda, interna ou exclusiva.");
                        // ✅ Som de erro
                        if (plugin.getConfig().getBoolean("Settings.EnableSounds", true) && sender instanceof Player) {
                            Player p = (Player) sender;
                            String sound = plugin.getConfig().getString("Sounds.Admin.Error", "block.note_block.bass");
                            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                        }
                        return true;
                    }

                    List<String> keysApagadas = new ArrayList<>();
                    if (keysConfig.contains("keys")) {
                        for (String key : new ArrayList<>(keysConfig.getConfigurationSection("keys").getKeys(false))) {
                            String keyOrigem = keysConfig.getString("keys." + key + ".origem", "");
                            if (origem.equals(keyOrigem)) {
                                String tipo = keysConfig.getString("keys." + key + ".tipo", "desconhecido");
                                String dono = keysConfig.contains("keys." + key + ".exclusivo_para.nome")
                                        ? keysConfig.getString("keys." + key + ".exclusivo_para.nome")
                                        : null;

                                keysConfig.set("keys." + key, null);
                                keysApagadas.add(key + "|" + tipo + "|" + (dono != null ? dono : "n/a"));
                            }
                        }
                    }
                    plugin.getConfigManager().saveKeys();

                    // Log individual
                    for (String logData : keysApagadas) {
                        String[] parts = logData.split("\\|", 3);
                        String key = parts[0];
                        String tipo = parts[1];
                        String dono = parts[2];
                        String details = "Tipo: " + tipo + " | Origem: " + origem;
                        if (!"n/a".equals(dono)) details += " | Dono: " + dono;
                        logKey(key, "APAGADA", details, sender.getName());
                    }

                    sender.sendMessage(ColorUtils.translate("&a✓ Removidas &f" + keysApagadas.size() + " &akeys de origem '&f" + origem + "&a'."));
                    // ✅ Som de sucesso
                    if (plugin.getConfig().getBoolean("Settings.EnableSounds", true) && sender instanceof Player) {
                        Player p = (Player) sender;
                        String sound = plugin.getConfig().getString("Sounds.Admin.KeyDeleted", "block.note_block.bass");
                        p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                    }
                    return true;

                } else if ("tipo".equals(alvo) && args.length >= 3) {
                    String tipoAlvo = args[2].toLowerCase();
                    List<String> keysApagadas = new ArrayList<>();
                    if (keysConfig.contains("keys")) {
                        for (String key : new ArrayList<>(keysConfig.getConfigurationSection("keys").getKeys(false))) {
                            String tipo = keysConfig.getString("keys." + key + ".tipo", "").toLowerCase();
                            if (tipoAlvo.equals(tipo)) {
                                String origem = keysConfig.getString("keys." + key + ".origem", "desconhecida");
                                String dono = keysConfig.contains("keys." + key + ".exclusivo_para.nome")
                                        ? keysConfig.getString("keys." + key + ".exclusivo_para.nome")
                                        : null;

                                keysConfig.set("keys." + key, null);
                                keysApagadas.add(key + "|" + tipo + "|" + origem + "|" + (dono != null ? dono : "n/a"));
                            }
                        }
                    }
                    plugin.getConfigManager().saveKeys();

                    for (String logData : keysApagadas) {
                        String[] parts = logData.split("\\|", 4);
                        String key = parts[0];
                        String tipo = parts[1];
                        String origem = parts[2];
                        String dono = parts[3];
                        String details = "Tipo: " + tipo + " | Origem: " + origem;
                        if (!"n/a".equals(dono)) details += " | Dono: " + dono;
                        logKey(key, "APAGADA", details, sender.getName());
                    }

                    sender.sendMessage(ColorUtils.translate("&a✓ Removidas &f" + keysApagadas.size() + " &akeys do tipo '&f" + tipoAlvo + "&a'."));
                    // ✅ Som de sucesso
                    if (plugin.getConfig().getBoolean("Settings.EnableSounds", true) && sender instanceof Player) {
                        Player p = (Player) sender;
                        String sound = plugin.getConfig().getString("Sounds.Admin.KeyDeleted", "block.note_block.bass");
                        p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                    }
                    return true;

                } else if ("player".equals(alvo) && args.length >= 3) {
                    String playerNome = args[2];
                    List<String> keysApagadas = new ArrayList<>();
                    if (keysConfig.contains("keys")) {
                        for (String key : new ArrayList<>(keysConfig.getConfigurationSection("keys").getKeys(false))) {
                            String dono = keysConfig.getString("keys." + key + ".exclusivo_para.nome", "");
                            if (playerNome.equalsIgnoreCase(dono)) {
                                String tipo = keysConfig.getString("keys." + key + ".tipo", "desconhecido");
                                keysConfig.set("keys." + key, null);
                                keysApagadas.add(key + "|" + tipo + "|" + dono);
                            }
                        }
                    }
                    plugin.getConfigManager().saveKeys();

                    for (String logData : keysApagadas) {
                        String[] parts = logData.split("\\|", 3);
                        String key = parts[0];
                        String tipo = parts[1];
                        String dono = parts[2];
                        String details = "Tipo: " + tipo + " | Origem: exclusiva | Dono: " + dono;
                        logKey(key, "APAGADA", details, sender.getName());
                    }

                    sender.sendMessage(ColorUtils.translate("&a✓ Removidas &f" + keysApagadas.size() + " &akeys exclusivas de '&f" + playerNome + "&a'."));
                    // ✅ Som de sucesso
                    if (plugin.getConfig().getBoolean("Settings.EnableSounds", true) && sender instanceof Player) {
                        Player p = (Player) sender;
                        String sound = plugin.getConfig().getString("Sounds.Admin.KeyDeleted", "block.note_block.chime"); // ← Usar KeyDeleted
                        p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                    }
                    return true;

                } else if ("tudo".equals(alvo)) {
                    int total = 0;
                    if (keysConfig.contains("keys")) {
                        total = keysConfig.getConfigurationSection("keys").getKeys(false).size();
                    }

                    sender.sendMessage(ColorUtils.translate("&c[===============================]"));
                    sender.sendMessage(ColorUtils.translate("&c|          &4&l⚠ ATENÇÃO! ⚠             &c|"));
                    sender.sendMessage(ColorUtils.translate("&c|                                               |"));
                    sender.sendMessage(ColorUtils.translate("&c| &eVocê está prestes a apagar          &c|"));
                    sender.sendMessage(ColorUtils.translate("&c|  &f" + total + "  &ekeys do sistema!                     &c|"));
                    sender.sendMessage(ColorUtils.translate("&c|                                              |"));
                    sender.sendMessage(ColorUtils.translate("&c|  &4&l❗ Isso não pode ser desfeito!&c|"));
                    sender.sendMessage(ColorUtils.translate("&c|                                               |"));
                    sender.sendMessage(ColorUtils.translate("&c| &aPara confirmar, use:                    &c|"));
                    sender.sendMessage(ColorUtils.translate("&c| &f/kamikeys confirmar apagar_tudo    &c|"));
                    sender.sendMessage(ColorUtils.translate("&c[===============================]"));

                    // ✅ Som de confirmação (alerta)
                    if (plugin.getConfig().getBoolean("Settings.EnableSounds", true) && sender instanceof Player) {
                        Player p = (Player) sender;
                        String sound = plugin.getConfig().getString("Sounds.Admin.ConfirmationNeeded", "entity.ender_dragon.growl");
                        p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                    }
                    return true;

                } else {
                    sender.sendMessage("§cSubcomando desconhecido. Use: origem, tipo, player ou tudo");
                    // ✅ Som de erro
                    if (plugin.getConfig().getBoolean("Settings.EnableSounds", true) && sender instanceof Player) {
                        Player p = (Player) sender;
                        String sound = plugin.getConfig().getString("Sounds.Admin.Error", "block.note_block.bass");
                        p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                    }
                    return true;
                }

            }

            case "list": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cComando só para jogadores.");
                    return true;
                }

                Player player = (Player) sender;
                if (player.hasPermission("kamikeys.admin")) {
                    // ✅ Abre o MENU PRINCIPAL (não a GUI antiga)
                    AdminMainMenu mainMenu = new AdminMainMenu(plugin, player);
                    plugin.getAdminKeysGUIs().put(player.getUniqueId(), mainMenu);
                    mainMenu.open();
                    return true;
                } else {
                    player.sendMessage(ColorUtils.translate("&c❌ Você não tem permissão para isso!"));
                    return true;
                }
            }

            case "confirmar": {
                if (args.length < 2 || !"apagar_tudo".equals(args[1])) {
                    sender.sendMessage("§cUso: /kamikeys confirmar apagar_tudo");
                    // ✅ Som de erro (comando inválido)
                    if (plugin.getConfig().getBoolean("Settings.EnableSounds", true) && sender instanceof Player) {
                        Player p = (Player) sender;
                        String sound = plugin.getConfig().getString("Sounds.Admin.Error", "block.note_block.bass");
                        p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                    }
                    return true;
                }

                FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
                int total = 0;
                if (keysConfig.contains("keys")) {
                    total = keysConfig.getConfigurationSection("keys").getKeys(false).size();
                    keysConfig.set("keys", null);
                }
                plugin.getConfigManager().saveKeys();

                sender.sendMessage(ColorUtils.translate("&c✓ Todas as &f" + total + " &ckeys foram apagadas!"));

                // ✅ Som de sucesso (após apagar)
                if (plugin.getConfig().getBoolean("Settings.EnableSounds", true) && sender instanceof Player) {
                    Player p = (Player) sender;
                    String sound = plugin.getConfig().getString("Sounds.Admin.Success", "block.note_block.chime");
                    p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                }

                // Log
                try (PrintWriter out = new PrintWriter(new FileWriter(plugin.getConfigManager().getLogsFile(), true))) {
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                    out.println("[" + now.format(formatter) + "] [APAGADAS_TODAS] " + total + " keys | Por: " + sender.getName());
                    out.println();
                } catch (IOException e) {
                    plugin.getLogger().severe("Erro ao logar apagar tudo: " + e.getMessage());
                }
                return true;
            }


            case "exportar": {
                try {
                    // Cria pasta de backups se não existir
                    File backupsDir = new File(plugin.getDataFolder(), "backups");
                    if (!backupsDir.exists()) {
                        backupsDir.mkdirs();
                    }

                    // Nome do arquivo com data/hora
                    LocalDateTime now = LocalDateTime.now();
                    String timestamp = now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm"));
                    File backupFile = new File(backupsDir, "backup_" + timestamp + ".yml");

                    YamlConfiguration backup = new YamlConfiguration();
                    Map<String, List<String>> global = new HashMap<>();
                    Map<String, Map<String, Object>> exclusivas = new HashMap<>();

                    FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
                    if (keysConfig.contains("keys")) {
                        for (String key : keysConfig.getConfigurationSection("keys").getKeys(false)) {
                            String tipo = keysConfig.getString("keys." + key + ".tipo", "desconhecido");
                            String origem = keysConfig.getString("keys." + key + ".origem", "");

                            String tipoFormatado = tipo.substring(0, 1).toUpperCase() + (tipo.length() > 1 ? tipo.substring(1).toLowerCase() : "");

                            if ("exclusiva".equals(origem)) {
                                String dono = keysConfig.getString("keys." + key + ".exclusivo_para.nome", "desconhecido");
                                String gerador = keysConfig.getString("keys." + key + ".gerador", "console");
                                Map<String, Object> info = new HashMap<>();
                                info.put("tipo", tipoFormatado);
                                info.put("dono", dono);
                                info.put("gerador", gerador);
                                exclusivas.put(key, info);
                            } else {
                                global.computeIfAbsent(tipoFormatado, k -> new ArrayList<>()).add(key);
                            }
                        }
                    }

                    backup.set("Global", global);
                    backup.set("Exclusivas", exclusivas);
                    backup.save(backupFile);

                    sender.sendMessage(ColorUtils.translate("&a✓ Backup visual salvo em: &fbackups/" + backupFile.getName()));
                } catch (Exception e) {
                    sender.sendMessage(ColorUtils.translate("&c✗ Erro ao gerar backup."));
                    plugin.getLogger().severe("Erro em /kamikeys exportar: " + e.getMessage());
                }
                return true;
            }

            default:
                sender.sendMessage("§cSubcomando desconhecido.");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("kamikeys.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> allCommands = List.of("venda", "gerar", "dar", "exportar", "apagar", "confirmar", "list", "reload");
            String current = args[0].toLowerCase();
            for (String cmd : allCommands) {
                if (cmd.startsWith(current)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            if ("venda".equals(args[0]) || "gerar".equals(args[0])) {
                if (plugin.getConfig().contains("Types")) {
                    Set<String> types = plugin.getConfig().getConfigurationSection("Types").getKeys(false);
                    completions.addAll(types);
                }
            } else if ("dar".equals(args[0])) {
                // Sugere nomes de jogadores que já entraram no servidor
                for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                    if (p.hasPlayedBefore()) {
                        completions.add(p.getName());
                    }
                }
            } else if ("apagar".equals(args[0])) {
                completions.add("origem");
                completions.add("tipo");
                completions.add("player");
                completions.add("tudo");
            } else if ("confirmar".equals(args[0])) {
                completions.add("apagar_tudo");
            }
        } else if (args.length == 3) {
            if ("dar".equals(args[0])) {
                // Sugere tipos de keys
                if (plugin.getConfig().contains("Types")) {
                    Set<String> types = plugin.getConfig().getConfigurationSection("Types").getKeys(false);
                    completions.addAll(types);
                }
            } else if ("apagar".equals(args[0])) {
                String subAlvo = args[1];
                if ("origem".equals(subAlvo)) {
                    completions.add("venda");
                    completions.add("interna");
                    completions.add("exclusiva");
                } else if ("tipo".equals(subAlvo)) {
                    if (plugin.getConfig().contains("Types")) {
                        Set<String> types = plugin.getConfig().getConfigurationSection("Types").getKeys(false);
                        completions.addAll(types);
                    }
                } else if ("player".equals(subAlvo)) {
                    for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                        if (p.hasPlayedBefore()) {
                            completions.add(p.getName());
                        }
                    }
                }
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
    private void logKey(String key, String action, String details, String actor) {
        try (PrintWriter out = new PrintWriter(new FileWriter(plugin.getConfigManager().getLogsFile(), true))) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            out.println("[" + now.format(formatter) + "] [" + action + "] Key: " + key);
            out.println("| " + details + " | Gerador: " + actor);
            out.println();
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao escrever no log: " + e.getMessage());
        }
    }

}