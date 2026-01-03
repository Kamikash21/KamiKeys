package com.kamiplugins.kamikeys.commands;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // Importado
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.List;

// A classe agora implementa as duas interfaces necessárias
public class AtivarCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public AtivarCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override // Anotação correta para o método onTabComplete da interface TabCompleter
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Não sugerimos chaves, apenas o padrão (nomes de jogadores)
        return null;
    }

    // Método auxiliar para encontrar a chave real no config.yml ignorando maiúsculas/minúsculas
    private String findConfigKeyIgnoreCase(String typeName) {
        if (plugin.getConfig().getConfigurationSection("Types") == null) {
            return null;
        }
        for (String key : plugin.getConfig().getConfigurationSection("Types").getKeys(false)) {
            if (key.equalsIgnoreCase(typeName)) {
                return key; // Retorna o nome real como está no config.yml (ex: "Basica" ou "basica")
            }
        }
        return null;
    }

    @Override // Anotação correta para o método onCommand da interface CommandExecutor
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Apenas jogadores podem usar o /ativar
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem ativar chaves.");
            return true;
        }

        Player player = (Player) sender;

        // Validação de argumento
        if (args.length != 1) {
            player.sendMessage(ColorUtils.translate("&cUso incorreto! Use: /ativar <key>"));
            return true;
        }

        String keyCode = args[0].toUpperCase();
        FileConfiguration keysConfig = plugin.getConfigManager().getKeysConfig();
        String path = "keys." + keyCode;

        // Verifica se a key existe no arquivo keys.yml
        if (!keysConfig.contains(path)) {
            player.sendMessage(ColorUtils.translate("&cChave inválida ou já utilizada!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        // Verifica se a chave é exclusiva para outro jogador
        String exclusivoPara = keysConfig.getString(path + ".exclusivo_para");
        if (exclusivoPara != null && !exclusivoPara.equalsIgnoreCase(player.getName())) {
            player.sendMessage(ColorUtils.translate("&cEsta chave é exclusiva para outro jogador!"));
            return true;
        }

        // Pega o tipo da chave como está salvo (ex: "basica")
        String tipoSalvo = keysConfig.getString(path + ".tipo");

        // Use o novo método para encontrar o nome exato no config.yml (ex: "Basica")
        String configKeyName = findConfigKeyIgnoreCase(tipoSalvo);

        // Se a chave não for encontrada no config.yml (mesmo ignorando case), mostre o erro
        if (configKeyName == null) {
            player.sendMessage(ColorUtils.translate("&cErro: O tipo de chave '" + tipoSalvo + "' não está configurado no config.yml!"));
            return true;
        }


        String prefixoChave = plugin.getKeyManager().getPrefixForType(configKeyName);

        // AGORA SIM, use a chave correta para buscar os comandos
        List<String> comandos = plugin.getConfig().getStringList("Types." + configKeyName + ".Commands");

        if (comandos == null || comandos.isEmpty()) {
            player.sendMessage(ColorUtils.translate("&cErro: Esta chave não possui comandos de recompensa configurados."));
            return true;
        }

        // EXECUÇÃO DAS RECOMPENSAS
        for (String cmd : comandos) {
            String finalCmd = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }

        // REMOÇÃO DA KEY (Sua especificação: excluir após ativada)
        keysConfig.set(path, null);
        plugin.getConfigManager().saveKeys();

        // FEEDBACK AO JOGADOR (ESTA LINHA FOI ALTERADA)
        player.sendMessage(ColorUtils.translate("&e&l[KamiKeys] " + prefixoChave + " &aChave &b" + tipoSalvo.toUpperCase() + " &aativada com sucesso!"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        // LOG NO CONSOLE
        plugin.getLogger().info("LOG: " + player.getName() + " resgatou a key " + keyCode + " (Tipo: " + tipoSalvo + ")");

        return true;
    }
}
