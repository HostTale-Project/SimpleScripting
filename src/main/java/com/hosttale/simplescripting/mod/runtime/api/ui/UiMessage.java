package com.hosttale.simplescripting.mod.runtime.api.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JS-visible message composed of one or more UiText fragments.
 */
public final class UiMessage {

    private final List<UiText> parts;

    public UiMessage(List<UiText> parts) {
        this.parts = Collections.unmodifiableList(new ArrayList<>(parts));
    }

    public List<UiText> getParts() {
        return parts;
    }

    public UiMessage concat(UiText... moreParts) {
        List<UiText> merged = new ArrayList<>(parts);
        merged.addAll(Arrays.asList(moreParts));
        return new UiMessage(merged);
    }

    static UiMessage fromParts(Object... parts) {
        List<UiText> out = new ArrayList<>();
        if (parts != null) {
            for (Object part : parts) {
                flattenPart(out, part);
            }
        }
        return new UiMessage(out);
    }

    private static void flattenPart(List<UiText> out, Object part) {
        if (part == null) {
            out.add(new UiText(""));
            return;
        }
        if (part instanceof UiText text) {
            out.add(text);
            return;
        }
        if (part instanceof UiMessage message) {
            out.addAll(message.getParts());
            return;
        }
        out.add(new UiText(String.valueOf(part)));
    }
}
