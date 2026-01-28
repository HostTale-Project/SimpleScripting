package com.hosttale.simplescripting.mod.runtime.api.ui;

/**
 * Simple text helpers that stay within plain JS types.
 */
public final class UiApi {

    /**
     * Create a text fragment with no styling.
     */
    public UiText raw(String text) {
        return new UiText(text);
    }

    /**
     * Create a colored text fragment.
     */
    public UiText color(String text, String color) {
        return new UiText(text).color(color);
    }

    /**
     * Join text fragments (strings, UiText, UiMessage) into a single message.
     */
    public UiMessage join(Object... parts) {
        return UiMessage.fromParts(parts);
    }
}
