package com.kamiplugins.kamikeys.utils;

import java.util.HashMap;
import java.util.Map;

public class MessageUtils {

    public static String applyPlaceholders(String message, Map<String, String> placeholders) {
        if (message == null) return null;

        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public static String applyColor(String message) {
        return ColorUtils.translate(message);
    }

    public static Map<String, String> createPlaceholders(Object... pairs) {
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i + 1 < pairs.length) {
                placeholders.put(pairs[i].toString(), pairs[i + 1].toString());
            }
        }
        return placeholders;
    }
}