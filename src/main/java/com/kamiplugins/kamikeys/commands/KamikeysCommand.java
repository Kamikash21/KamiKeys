package com.kamiplugins.kamikeys.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;

public class KamikeysCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        sender.sendMessage(ChatColor.DARK_PURPLE + "§m-------" + ChatColor.LIGHT_PURPLE + " [ KamiKeys ] " + ChatColor.DARK_PURPLE + "§m-------");
        sender.sendMessage("§e/ativar (key) §8- §fAtiva uma key");

        // Apenas mostra comandos administrativos se o sender tiver permissão
        if (sender.hasPermission("kamikeys.admin")) {
            sender.sendMessage("§e/gerarkey (tipo) (quant) §8- §fGera novas keys");
            sender.sendMessage("§e/darkey (player) (tipo) (quant) §8- §fDá key a um player");
            sender.sendMessage("§e/apagarkey (tipo) §8- §fApaga keys de um tipo");
            sender.sendMessage("§e/key list  §8- §fLista keys ativas");
            sender.sendMessage("§e/key reload §8- §fRecarrega o plugin");
        }

        sender.sendMessage(ChatColor.DARK_PURPLE + "§m-----------------------------");

        return true;
    }
}
