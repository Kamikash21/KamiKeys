package com.kamiplugins.kamikeys.utils;

import org.bukkit.ChatColor;

public class ColorUtils {

    /**
     * Traduz códigos de cores do formato '&' para o formato '§' do Minecraft.
     * @param message A string original com códigos '&'.
     * @return A string traduzida.
     */
    public static String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
