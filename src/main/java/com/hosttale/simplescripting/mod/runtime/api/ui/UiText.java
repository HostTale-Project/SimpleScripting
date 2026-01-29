package com.hosttale.simplescripting.mod.runtime.api.ui;

/**
 * JS-visible text fragment that can carry styling (color only for now).
 */
public final class UiText {
    private final String text;
    private final String color;

    public UiText(String text) {
        this(text, null);
    }

    private UiText(String text, String color) {
        this.text = text == null ? "" : text;
        this.color = sanitize(color);
    }

    public String getText() {
        return text;
    }

    public String getColor() {
        return color;
    }

    /**
     * Returns a new UiText with the given color (hex or named).
     */
    public UiText color(String colorHex) {
        return new UiText(text, colorHex);
    }

    UiText withoutColor() {
        return new UiText(text, null);
    }

    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }
}
