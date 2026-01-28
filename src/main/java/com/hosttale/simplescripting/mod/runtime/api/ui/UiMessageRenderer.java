package com.hosttale.simplescripting.mod.runtime.api.ui;

import com.hypixel.hytale.server.core.Message;

import java.util.List;

/**
 * Converts JS-facing UI message objects into native Message instances.
 */
public final class UiMessageRenderer {

    private UiMessageRenderer() {
    }

    public static Message toMessage(Object value) {
        if (value == null) {
            return Message.raw("");
        }
        if (value instanceof UiMessage message) {
            return toMessage(message);
        }
        if (value instanceof UiText text) {
            return toMessage(text);
        }
        if (value instanceof CharSequence seq) {
            return Message.raw(seq.toString());
        }
        return Message.raw(String.valueOf(value));
    }

    public static Message toMessage(UiMessage message) {
        List<UiText> parts = message.getParts();
        if (parts.isEmpty()) {
            return Message.raw("");
        }
        Message[] rendered = new Message[parts.size()];
        for (int i = 0; i < parts.size(); i++) {
            rendered[i] = toMessage(parts.get(i));
        }
        return Message.join(rendered);
    }

    public static Message toMessage(UiText text) {
        Message message = Message.raw(text.getText());
        if (text.getColor() != null) {
            message.color(text.getColor());
        }
        return message;
    }
}
