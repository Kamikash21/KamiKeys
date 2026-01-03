package com.kamiplugins.kamikeys.commands;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors; // Import necessário
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor; // Já deve ter algo similar para cores


public class GiveKeyCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public GiveKeyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kamikeys.admin.give")) {
            sender.sendMessage(ColorUtils.translate(plugin.getConfig().getString("Messages.NoPermission")));
            return true;
        }

        // Validação: Espera 2 ou 3 argumentos (/darkey <jogador> <tipo> [quantidade])
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(ColorUtils.translate("&cUso: /darkey <jogador> <tipo> [quantidade]"));
            return true;
        }

        // CORRIGIDO: Usa args[0] para o nome do jogador
        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage(ColorUtils.translate("&cJogador offline ou não encontrado."));
            return true;
        }

        // CORRIGIDO: Usa args[1] para o tipo e aplica toLowerCase()
        String tipo = args[1].toLowerCase();
        int quantidade = 1;

        if (args.length == 3) {
            try {
                // CORRIGIDO: Usa args[2] para a quantidade
                quantidade = Integer.parseInt(args[2]);
                if (quantidade <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(ColorUtils.translate("&cA quantidade deve ser um número inteiro positivo!"));
                return true;
            }
        }

        // --- LÓGICA DE VALIDAÇÃO E DAR A KEY (AGORA CASE-INSENSITIVE) ---
        Set<String> tiposValidos = plugin.getConfig().getConfigurationSection("Types").getKeys(false);
        boolean tipoExiste = tiposValidos.stream()
                .anyMatch(t -> t.equalsIgnoreCase(tipo));

        if (!tipoExiste) {
            sender.sendMessage(ColorUtils.translate("&cTipo de chave inválido! Use /gerarkey para ver os tipos."));
            return true;
        }

        // Recupera o nome exato da chave como está na config (ex: "Basica" em vez de "basica")
        String tipoFinal = tiposValidos.stream()
                .filter(t -> t.equalsIgnoreCase(tipo))
                .findFirst()
                .orElse(tipo);

        // Agora usa tipoFinal para todo o resto

        // --- LÓGICA DE DAR A KEY ---
        String prefixoChave = plugin.getKeyManager().getPrefixForType(tipoFinal);

        // Use uma lista para armazenar as chaves geradas para a mensagem clicável
        List<String> chavesGeradas = new ArrayList<>();

        for (int i = 0; i < quantidade; i++) {
            String newKey = plugin.getKeyManager().generateRandomKey(tipoFinal);
            plugin.getConfigManager().getKeysConfig().set("keys." + newKey + ".tipo", tipoFinal);
            plugin.getConfigManager().getKeysConfig().set("keys." + newKey + ".gerador", sender.getName());
            chavesGeradas.add(newKey); // Adiciona a chave à lista
        }
        plugin.getConfigManager().saveKeys();

        // --- FIM DA LÓGICA DE DAR A KEY ---

        // Feedback para o remetente (continua a mesma)
        sender.sendMessage(ColorUtils.translate("&e&l[KamiKeys] &aVocê deu &f" + quantidade + "x &achaves " + prefixoChave + " &apara &b" + targetPlayer.getName() + "&a!"));

        // NOVO Feedback para o jogador com mensagens separadas
        for (String key : chavesGeradas) {
            // 1ª MENSAGEM: Informação sobre a chave recebida
            TextComponent infoMessage = new TextComponent(ColorUtils.translate("&e&l[KamiKeys] &aVocê recebeu uma chave " + prefixoChave + "!"));
            targetPlayer.spigot().sendMessage(infoMessage);

            // 2ª MENSAGEM: O botão clicável
            TextComponent clickText = new TextComponent(ColorUtils.translate("&b&l[Clique para ativar sua key]"));

            // Configura a ação de clique para a key específica deste loop
            clickText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ativar " + key));

            // Opcional: Adiciona um texto ao passar o mouse para ficar mais profissional
            clickText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new net.md_5.bungee.api.chat.hover.content.Text("§7Clique para ativar: §f" + key)));

            // Envia a segunda mensagem separadamente
            targetPlayer.spigot().sendMessage(clickText);
        }

        return true;

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("kamikeys.admin.give")) return null;

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            if (plugin.getConfig().getConfigurationSection("Types") != null) {
                Set<String> tiposValidos = plugin.getConfig().getConfigurationSection("Types").getKeys(false);
                for (String tipo : tiposValidos) {
                    if (tipo.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(tipo);
                    }
                }
            }
        }

        return completions;
    }
}
