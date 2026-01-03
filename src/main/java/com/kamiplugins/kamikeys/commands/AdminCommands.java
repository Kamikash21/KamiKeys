package com.kamiplugins.kamikeys.commands;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Set;

public class AdminCommands implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public AdminCommands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("kamikeys.admin")) return null;

        List<String> completions = new java.util.ArrayList<>();
        Set<String> tiposValidos = plugin.getConfig().getConfigurationSection("Types").getKeys(false);

        if (args.length == 1) {
            List<String> subCommands = java.util.Arrays.asList("gerar", "dar", "list", "apagar", "reload", "help");
            for (String cmd : subCommands) {
                if (cmd.toLowerCase().startsWith(args[0].toLowerCase())) completions.add(cmd);
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("gerar") || subCommand.equals("list") || subCommand.equals("apagar") || subCommand.equals("dar")) {
                for (String tipo : tiposValidos) {
                    if (tipo.toLowerCase().startsWith(args[1].toLowerCase())) completions.add(tipo);
                }
                if (subCommand.equals("apagar")) completions.add("tudo");
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kamikeys.admin")) {
            sender.sendMessage(ColorUtils.translate(plugin.getConfig().getString("Messages.NoPermission", "&cSem permissão.")));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            enviarAjuda(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "gerar":
                if (args.length != 3) { sender.sendMessage(ColorUtils.translate("&cUso: /key gerar <tipo> <quantidade>")); return true; }
                processarGerar(sender, args[1].toLowerCase(), args[2]); return true;
            case "dar":
                if (args.length != 3) { sender.sendMessage(ColorUtils.translate("&cUso: /key dar <player> <tipo>")); return true; }
                processarDar(sender, args[1], args[2].toLowerCase()); return true;
            case "list":
                // Lógica da listagem está inline ou em método privado, vamos para o método privado:
                processarListar(sender); return true; // Método privado para listagem completa
            case "apagar":
                if (args.length != 2) { sender.sendMessage(ColorUtils.translate("&cUso: /key apagar <tipo>")); return true; }
                processarApagar(sender, args[1].toLowerCase()); return true; // Método privado para apagar
            case "reload":
                plugin.reloadConfig(); plugin.getConfigManager().reloadConfigs();
                sender.sendMessage(ColorUtils.translate("&aConfigurações recarregadas!")); return true;
            default:
                sender.sendMessage(ColorUtils.translate("&cComando inválido. Use /key help.")); return true;
        }
    }

    // --- Métodos Auxiliares Privados (Completos usando ConfigManager) ---

    private void processarGerar(CommandSender sender, String tipo, String qtdStr) {
        int quantidade;
        try {
            quantidade = Integer.parseInt(qtdStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.translate("&cA quantidade deve ser um número válido!")); return;
        }
        if (!validarTipo(tipo)) { sender.sendMessage(ColorUtils.translate("&cTipo de chave inválido!")); return; }

        for (int i = 0; i < quantidade; i++) {
            String key = plugin.getKeyManager().generateRandomKey(tipo);
            plugin.getConfigManager().getKeysConfig().set("keys." + key + ".tipo", tipo);
            plugin.getConfigManager().getKeysConfig().set("keys." + key + ".gerador", sender.getName());
        }
        plugin.getConfigManager().saveKeys(); // Salva no keys.yml
        sender.sendMessage(ColorUtils.translate("&a&l[KamiKeys] &f" + quantidade + " keys &agendas no keys.yml."));
    }

    private void processarDar(CommandSender sender, String playerName, String tipo) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) { sender.sendMessage(ColorUtils.translate("&cJogador offline.")); return; }
        if (!validarTipo(tipo)) { sender.sendMessage(ColorUtils.translate("&cTipo de chave inválido!")); return; }

        String key = plugin.getKeyManager().generateRandomKey(tipo);
        plugin.getConfigManager().getKeysConfig().set("keys." + key + ".tipo", tipo);
        plugin.getConfigManager().getKeysConfig().set("keys." + key + ".exclusivo_para", target.getName());
        plugin.getConfigManager().saveKeys();

        sender.sendMessage(ColorUtils.translate("&aKey exclusiva gerada: &f" + key));
        target.sendMessage(ColorUtils.translate("&bVocê recebeu uma key! Ative com: &f/ativar " + key));
    }

    private void processarListar(CommandSender sender) {
        FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
        ConfigurationSection keysSection = keysConfig.getConfigurationSection("keys");

        if (keysSection == null || keysSection.getKeys(false).isEmpty()) {
            sender.sendMessage(ColorUtils.translate("&cNenhuma chave ativa encontrada."));
            return;
        }

        sender.sendMessage(ColorUtils.translate("&e&l--- Lista de Chaves Ativas ---"));

        for (String key : keysSection.getKeys(false)) {
            String tipo = keysSection.getString(key + ".tipo", "N/A");
            String exclusivo = keysSection.getString(key + ".exclusivo_para", "Global");

            // Obtenha o prefixo formatado usando o KeyManager
            String prefixoChave = plugin.getKeyManager().getPrefixForType(tipo);

            // --- CONSTRUÇÃO DA MENSAGEM CLICÁVEL ---

            // 1. O código da chave (a parte clicável)
            TextComponent keyComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', "&b" + key));

            // Adiciona a ação de clique: SUGERIR COMANDO (copia para o chat, não executa)
            keyComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, key));

            // Adiciona um texto ao passar o mouse (opcional)
            keyComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', "&aClique para copiar a chave!")).create()));

            // 2. O restante da mensagem (texto normal)
            TextComponent restante = new TextComponent(
                    ColorUtils.translate("&7| Tipo: " + prefixoChave + " &7| Dono: " + exclusivo)
            );

            // 3. Monta a linha completa: "- " + keyComponent + restante
            TextComponent linhaCompleta = new TextComponent(ColorUtils.translate("&f- "));
            linhaCompleta.addExtra(keyComponent);
            linhaCompleta.addExtra(restante);

            // Envia a mensagem completa para o jogador (que agora suporta clique)
            if (sender instanceof Player) {
                ((Player) sender).spigot().sendMessage(linhaCompleta);
            } else {
                // Caso seja o console usando o comando, envie a mensagem simples
                sender.sendMessage(linhaCompleta.toLegacyText());
            }
        }
    }



    private void processarApagar(CommandSender sender, String tipoAlvo) {
        FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
        ConfigurationSection keysSection = keysConfig.getConfigurationSection("keys");
        if (keysSection == null) { sender.sendMessage(ColorUtils.translate("&cNenhuma chave ativa encontrada.")); return; }

        int contador = 0;
        for (String key : keysSection.getKeys(false)) {
            String tipoDaKey = keysConfig.getString("keys." + key + ".tipo");
            if (tipoAlvo.equals("tudo") || (tipoDaKey != null && tipoDaKey.equals(tipoAlvo))) {
                keysConfig.set("keys." + key, null);
                contador++;
            }
        }
        plugin.getConfigManager().saveKeys();
        sender.sendMessage(ColorUtils.translate("&aLimpas " + contador + " chaves do tipo " + tipoAlvo));
    }


    private boolean validarTipo(String tipo) {
        if (plugin.getConfig().getConfigurationSection("Types") == null) return false;
        return plugin.getConfig().getConfigurationSection("Types").getKeys(false).contains(tipo);
    }

    private void enviarAjuda(CommandSender sender) {
        sender.sendMessage(ColorUtils.translate("&e&l--- KamiKeys Admin Help ---"));
        sender.sendMessage(ColorUtils.translate("&f/key gerar <tipo> <qtd> &7- Gera keys no YAML."));
        sender.sendMessage(ColorUtils.translate("&f/key dar <player> <tipo> &7- Dá uma key a um player."));
        sender.sendMessage(ColorUtils.translate("&f/key list &7- Lista keys no YAML."));
        sender.sendMessage(ColorUtils.translate("&f/key apagar <tipo> &7- Apaga keys do YAML."));
        sender.sendMessage(ColorUtils.translate("&f/key reload &7- Recarrega config/keys.yml."));
    }
}
