package com.kamiplugins.kamikeys.commands;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GerarKeyCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public GerarKeyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kamikeys.admin")) {
            sender.sendMessage(ColorUtils.translate(plugin.getConfig().getString("Messages.NoPermission")));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(ColorUtils.translate("&cUso incorreto! Use: /gerarkey <tipo> <quantidade>"));
            return true;
        }

        String tipo = args[0].toLowerCase();
        int quantidade;

        try {
            quantidade = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.translate("&cA quantidade deve ser um número válido!"));
            return true;
        }

        Set<String> tiposValidos = plugin.getConfig().getConfigurationSection("Types").getKeys(false);

        boolean tipoExiste = tiposValidos.stream()
                .anyMatch(t -> t.equalsIgnoreCase(tipo));

        if (!tipoExiste) {
            sender.sendMessage(ColorUtils.translate("&cTipo de chave inválido! Tipos disponíveis: &f" + String.join(", ", tiposValidos)));
            return true;
        }

        String prefixoChave = plugin.getKeyManager().getPrefixForType(tipo);

        sender.sendMessage(ColorUtils.translate("&e&l[KamiKeys] &aIniciando geração de &f" + quantidade + " &achaves do tipo &f" + prefixoChave + "&a..."));

        // --- LÓGICA DE SALVAMENTO ATUALIZADA PARA A NOVA ESTRUTURA YAML ---
        FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
        String path = "keys.Global." + tipo.substring(0, 1).toUpperCase() + tipo.substring(1); // Ex: keys.Global.Basicas

        for (int i = 0; i < quantidade; i++) {
            String key = plugin.getKeyManager().generateRandomKey(tipo);

            // Obter a lista existente ou criar uma nova se não existir
            List<String> keyList = keysConfig.getStringList(path);
            if (keyList == null) {
                keyList = new ArrayList<>();
            }
            // Adicionar a nova chave à lista
            keyList.add(key);
            // Salvar a lista de volta na configuração
            keysConfig.set(path, keyList);
            // NOTA: Gerador não é mais salvo nesta estrutura, apenas o tipo.
        }

        plugin.getConfigManager().saveKeys();
        // --- FIM DA LÓGICA ATUALIZADA ---

        sender.sendMessage(ColorUtils.translate("&e&l[KamiKeys] " + prefixoChave + " &aFinalizado! &f" + quantidade + " &achaves do tipo " + prefixoChave + " &asalvas."));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // ... (onTabComplete permanece o mesmo) ...
        if (!sender.hasPermission("kamikeys.admin")) return null;

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            ConfigurationSection typesSection = plugin.getConfig().getConfigurationSection("Types");
            if (typesSection != null) {
                Set<String> tiposValidos = typesSection.getKeys(false);
                for (String tipo : tiposValidos) {
                    if (tipo.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(tipo);
                    }
                }
            }
            return completions;
        }
        return null;
    }
}
