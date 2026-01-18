package com.hosttale.simplescripting.util;

import com.hosttale.simplescripting.api.Colors;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import static com.hosttale.simplescripting.api.Colors.COLOR_MAP;

/**
 * Helper class to send messages to players from JavaScript.
 * Simplifies the Message API for JavaScript usage.
 * Supports color codes using & prefix (e.g., &a for green, &c for red).
 */
public class MessageHelper {
    
    /**
     * Translates & color codes to § color codes.
     * @param text The text with & color codes (e.g., "&aHello &cWorld")
     * @return Text with § color codes
     */
    public static Message colorize(String text) {
        Message message = Message.empty();
        StringBuilder buffer = new StringBuilder();
        Colors currentColor = Colors.WHITE;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                Colors newColor = COLOR_MAP.get(code);

                if (newColor != null) {
                    if (!buffer.isEmpty()) {
                        message = Message.join(
                                message,
                                Message.raw(buffer.toString()).color(currentColor.getHex())
                        );
                        buffer.setLength(0);
                    }
                    currentColor = newColor;
                    i++;
                    continue;
                }
            }

            buffer.append(c);
        }

        if (!buffer.isEmpty()) {
            message = Message.join(
                    message,
                    Message.raw(buffer.toString()).color(currentColor.getHex())
            );
        }

        return message;
    }
    
    /**
     * Creates a raw message from a string (no color processing).
     * @param text The text content
     * @return A Message object
     */
    public static Message raw(String text) {
        return colorize(text);
    }
    
    /**
     * Sends a message to a command sender (player/console) with color code support.
     * @param sender The command sender
     * @param message The message text (supports & color codes)
     */
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }
    
    /**
     * Sends a message to a player with color code support.
     * @param player The player
     * @param message The message text (supports & color codes)
     */
    public static void send(PlayerRef player, String message) {
        player.sendMessage(colorize(message));
    }
    
    /**
     * Sends a message to a command sender with color code support.
     * @param sender The command sender
     * @param message The message text (supports & color codes)
     */
    public static void send(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }
    
    /**
     * Sends a raw message to a player (no color processing).
     * @param player The player
     * @param message The Message object
     */
    public static void sendRaw(PlayerRef player, Message message) {
        player.sendMessage(message);
    }
    
    /**
     * Sends a raw message to a command sender (no color processing).
     * @param sender The command sender
     * @param message The Message object
     */
    public static void sendRaw(CommandSender sender, Message message) {
        sender.sendMessage(message);
    }
}
