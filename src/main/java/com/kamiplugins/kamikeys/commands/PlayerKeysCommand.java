package com.kamiplugins.kamikeys.commands;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.gui.PlayerKeysGUI;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerKeysCommand implements CommandExecutor {

    private final Main plugin;

    public PlayerKeysCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cComando só para jogadores.");
            return true;
        }
        Player player = (Player) sender;
        PlayerKeysGUI gui = new PlayerKeysGUI(plugin, player);
        plugin.getPlayerKeysGUIs().put(player.getUniqueId(), gui); // Armazena a GUI
        gui.open();
        return true;
    }
}