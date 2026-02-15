package com.kamiplugins.kamikeys.commands;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.gui.PlayerKeysGUI;
import com.kamiplugins.kamikeys.services.KeyService;
import com.kamiplugins.kamikeys.utils.MessageUtils;
import com.kamiplugins.kamikeys.utils.UxService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerKeysCommand implements CommandExecutor {
    private final Main plugin;
    private final KeyService keyService;
    private final UxService uxService;

    public PlayerKeysCommand(Main plugin) {
        this.plugin = plugin;
        this.keyService = plugin.getKeyService();
        this.uxService = plugin.getConfigManager().getUxService();
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

        // Abrir o menu de keys do jogador
        new PlayerKeysGUI(plugin, player, plugin.getKeyService(), plugin.getVoucherService(), plugin.getValidationService())
                .open(0); // Abrir na página 0

        // Som de abertura do menu
        uxService.playSoundFromConfig(player, "open_menu");

        return true;
    }

    public void refreshKeysMenu(Player player, int page) {
        new PlayerKeysGUI(
                plugin,
                player,
                plugin.getKeyService(),
                plugin.getVoucherService(),
                plugin.getValidationService()
        ).open(page);
    }

}