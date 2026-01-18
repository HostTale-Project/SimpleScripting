package com.hosttale.simplescripting.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Color enum for chat message formatting.
 * Maps Minecraft color codes (&0-&f) to hexadecimal colors.
 */
public enum Colors {
    // Minecraft color codes mapped to hex values
    BLACK("#000000"),        // &0
    DARK_BLUE("#0000AA"),    // &1
    DARK_GREEN("#00AA00"),   // &2
    DARK_AQUA("#00AAAA"),    // &3
    DARK_RED("#AA0000"),     // &4
    DARK_PURPLE("#AA00AA"),  // &5
    GOLD("#FFAA00"),         // &6
    GRAY("#AAAAAA"),         // &7
    DARK_GRAY("#555555"),    // &8
    BLUE("#5555FF"),         // &9
    GREEN("#55FF55"),        // &a
    AQUA("#55FFFF"),         // &b
    RED("#FF5555"),          // &c
    LIGHT_PURPLE("#FF55FF"), // &d
    YELLOW("#FFFF55"),       // &e
    WHITE("#FFFFFF");        // &f

    private final String hexColor;

    Colors(String hexColor) {
        this.hexColor = hexColor;
    }

    /**
     * Gets the hexadecimal color value as a string.
     * @return The hex color string (e.g., "#FFFFFF")
     */
    public String getHex() {
        return hexColor;
    }

    /**
     * Map of color code characters to Colors enum values.
     * Supports &0-&9 and &a-&f (case insensitive).
     */
    public static final Map<Character, Colors> COLOR_MAP = new HashMap<>();

    static {
        COLOR_MAP.put('0', BLACK);
        COLOR_MAP.put('1', DARK_BLUE);
        COLOR_MAP.put('2', DARK_GREEN);
        COLOR_MAP.put('3', DARK_AQUA);
        COLOR_MAP.put('4', DARK_RED);
        COLOR_MAP.put('5', DARK_PURPLE);
        COLOR_MAP.put('6', GOLD);
        COLOR_MAP.put('7', GRAY);
        COLOR_MAP.put('8', DARK_GRAY);
        COLOR_MAP.put('9', BLUE);
        COLOR_MAP.put('a', GREEN);
        COLOR_MAP.put('b', AQUA);
        COLOR_MAP.put('c', RED);
        COLOR_MAP.put('d', LIGHT_PURPLE);
        COLOR_MAP.put('e', YELLOW);
        COLOR_MAP.put('f', WHITE);
    }

    // Common aliases for convenience
    public static final Colors SUCCESS = GREEN;
    public static final Colors ERROR = RED;
    public static final Colors WARNING = YELLOW;
    public static final Colors INFO = AQUA;
    public static final Colors HIGHLIGHT = GOLD;
    public static final Colors MUTED = GRAY;

    /**
     * Strips all color codes from a string.
     * @param text The text with color codes
     * @return Text without color codes
     */
    public static String strip(String text) {
        if (text == null) return null;
        return text.replaceAll("&[0-9a-fA-Fk-oK-OrR]", "");
    }

    /**
     * Creates a colored message string with success styling.
     * @param message The message
     * @return Formatted message string
     */
    public static String success(String message) {
        return "&a" + message + "&r";
    }

    /**
     * Creates a colored message string with error styling.
     * @param message The message
     * @return Formatted message string
     */
    public static String error(String message) {
        return "&c" + message + "&r";
    }

    /**
     * Creates a colored message string with warning styling.
     * @param message The message
     * @return Formatted message string
     */
    public static String warning(String message) {
        return "&e" + message + "&r";
    }

    /**
     * Creates a colored message string with info styling.
     * @param message The message
     * @return Formatted message string
     */
    public static String info(String message) {
        return "&b" + message + "&r";
    }

    /**
     * Creates a message with a prefix.
     * @param prefix The prefix (will be colored gold)
     * @param message The message (will be white)
     * @return Formatted message string
     */
    public static String prefixed(String prefix, String message) {
        return "&6[" + prefix + "] &f" + message;
    }
}
