package com.hosttale.simplescripting.ui;

import com.hosttale.simplescripting.scripts.ScriptBrowser;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class ScriptViewerPage extends InteractiveCustomUIPage<ScriptViewerPage.ScriptViewerData> {

    private final ScriptBrowser browser;
    private final String scriptPath;
    private final String scriptName;
    private final String parentFolder;
    private String scriptContent;

    public ScriptViewerPage(@Nonnull PlayerRef playerRef,
                            @Nonnull ScriptBrowser browser,
                            @Nonnull String scriptPath) {
        super(playerRef, CustomPageLifetime.CanDismiss, ScriptViewerData.CODEC);
        this.browser = browser;
        this.scriptPath = normalizePath(scriptPath);
        this.scriptName = resolveName(this.scriptPath);
        this.parentFolder = extractFolder(this.scriptPath);
        loadScriptContent();
    }

    private void loadScriptContent() {
        try {
            scriptContent = browser.read(scriptPath);
        } catch (IOException | IllegalArgumentException e) {
            scriptContent = "// " + e.getMessage();
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder,
                      @Nonnull Store<EntityStore> store) {

        uiCommandBuilder.append("SimpleScripting/ScriptViewer.ui");
        uiCommandBuilder.set("#Title.Text", "Script: " + scriptName);
        uiCommandBuilder.set("#ScriptContent.Value", Objects.requireNonNullElse(scriptContent, ""));

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#BackBtn", EventData.of("Action", "back"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#SaveBtn", EventData.of("Action", "save"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#ReloadBtn", EventData.of("Action", "reload"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged,
                "#ScriptContent", EventData.of("@Content", "#ScriptContent.Value"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull ScriptViewerData data) {
        super.handleDataEvent(ref, store, data);

        if (data.content != null) {
            scriptContent = data.content;
        }

        if (data.action == null || data.action.isBlank()) {
            sendUpdate();
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef targetRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || targetRef == null) {
            return;
        }

        switch (data.action) {
            case "back" -> player.getPageManager().openCustomPage(ref, store,
                    new ScriptsListPage(targetRef, browser, CustomPageLifetime.CanDismiss, parentFolder));
            case "save" -> handleSave(targetRef);
            case "reload" -> handleReload(targetRef);
            default -> { }
        }

        sendUpdate();
    }

    private void handleSave(PlayerRef targetRef) {
        try {
            browser.write(scriptPath, scriptContent);
            targetRef.sendMessage(Message.raw("Saved " + scriptName).color("#00ff99"));
        } catch (IOException | IllegalArgumentException e) {
            targetRef.sendMessage(Message.raw("Error saving: " + e.getMessage()).color("#ff6666"));
        }
    }

    private void handleReload(PlayerRef targetRef) {
        ScriptBrowser.ReloadResult result = browser.reloadContainingMod(scriptPath);
        if (result.success()) {
            targetRef.sendMessage(Message.raw("Reloaded mod " + result.modId()).color("#00ff99"));
            loadScriptContent();
        } else {
            targetRef.sendMessage(Message.raw(result.error()).color("#ff6666"));
        }
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        return path.replace('\\', '/');
    }

    private String resolveName(String path) {
        try {
            Path p = Path.of(path);
            Path fileName = p.getFileName();
            if (fileName != null) {
                return fileName.toString();
            }
        } catch (Exception ignored) {
        }
        return path;
    }

    private String extractFolder(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "";
        }
        return path.substring(0, lastSlash);
    }

    public static class ScriptViewerData {
        public static final BuilderCodec<ScriptViewerData> CODEC = BuilderCodec.builder(ScriptViewerData.class, ScriptViewerData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("@Content", Codec.STRING), (d, v) -> d.content = v, d -> d.content).add()
                .build();

        private String action;
        private String content;
    }
}
