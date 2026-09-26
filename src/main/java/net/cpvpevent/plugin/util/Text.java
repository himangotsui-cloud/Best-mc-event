package net.cpvpevent.plugin.util;

import org.bukkit.ChatColor;

import java.util.Map;

/**
 * Small helper for colorizing and placeholder-replacing configured strings.
 */
public final class Text {

    private Text() {
    }

    public static String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static String replace(String input, Map<String, String> placeholders) {
        if (input == null) return "";
        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return color(result);
    }

    public static String replace(String input, String key, String value) {
        if (input == null) return "";
        return color(input.replace(key, value));
    }
}
