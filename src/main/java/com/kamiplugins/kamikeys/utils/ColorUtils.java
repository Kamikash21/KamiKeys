package com.kamiplugins.kamikeys.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Pattern;

public class ColorUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    public static String translate(String textToTranslate) {
        if (textToTranslate == null) return "";

        String coloredText = ChatColor.translateAlternateColorCodes('&', textToTranslate);

        // Suporte a cores hexadecimais
        if (coloredText.contains("#")) {
            java.util.regex.Matcher matcher = HEX_PATTERN.matcher(coloredText);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                String group = matcher.group();
                matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of(group).toString());
            }
            matcher.appendTail(buffer);
            coloredText = buffer.toString();
        }

        return coloredText;
    }
}