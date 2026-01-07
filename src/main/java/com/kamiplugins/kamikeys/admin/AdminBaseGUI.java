package com.kamiplugins.kamikeys.admin;

import com.kamiplugins.kamikeys.Main;
import org.bukkit.entity.Player;

public abstract class AdminBaseGUI {
    protected final Main plugin;
    protected final Player admin;

    public AdminBaseGUI(Main plugin, Player admin) {
        this.plugin = plugin;
        this.admin = admin;
    }

    public abstract void open();
    public abstract void refresh();
    public abstract int getCurrentPage();
    public abstract void nextPage();
    public abstract void previousPage();
}