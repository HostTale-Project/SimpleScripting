package com.hosttale.simplescripting.ui;

import com.hosttale.simplescripting.SimpleScriptingPlugin;
import com.hosttale.simplescripting.script.ScriptLoader;
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
import java.nio.file.Files;
import java.nio.file.Path;

public class ScriptViewerUI extends InteractiveCustomUIPage<ScriptViewerUI.ScriptViewerData> {

    private final PlayerRef playerRef;
    private final String scriptPath;
    private final String scriptName;
    private String scriptContent;

    public ScriptViewerUI(@Nonnull PlayerRef playerRef, @Nonnull String scriptPath) {
        super(playerRef, CustomPageLifetime.CanDismiss, ScriptViewerData.CODEC);
        this.playerRef = playerRef;
        this.scriptPath = scriptPath;
        this.scriptName = Path.of(scriptPath).getFileName().toString();
        loadScriptContent();
    }

    private void loadScriptContent() {
        try {
            ScriptLoader loader = SimpleScriptingPlugin.getInstance().getScriptLoader();
            if (loader != null) {
                var contextBuilder = loader.getContextBuilder();
                if (contextBuilder != null) {
                    Path modsPath = contextBuilder.getModsPath();
                    Path fullPath = modsPath.resolve(scriptPath);
                    if (Files.exists(fullPath)) {
                        scriptContent = Files.readString(fullPath);
                    } else {
                        scriptContent = "// File not found: " + scriptPath;
                    }
                    return;
                }
            }
            scriptContent = "// Error: Could not access mods directory";
        } catch (IOException e) {
            scriptContent = "// Error loading file: " + e.getMessage();
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                      @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        
        uiCommandBuilder.append("SimpleScripting/ScriptViewer.ui");
        
        // Set title
        uiCommandBuilder.set("#Title.Text", "Script: " + scriptName);
        
        // Set script content
        uiCommandBuilder.set("#ScriptContent.Value", scriptContent);
        
        // Bind button events
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
            "#BackBtn", EventData.of("Action", "back"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
            "#SaveBtn", EventData.of("Action", "save"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
            "#ReloadBtn", EventData.of("Action", "reload"));
        
        // Track content changes
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, 
            "#ScriptContent", EventData.of("@Content", "#ScriptContent.Value"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                                @Nonnull ScriptViewerData data) {
        super.handleDataEvent(ref, store, data);
        
        // Update content if changed
        if (data.content != null) {
            scriptContent = data.content;
        }
        
        if (data.action != null && !data.action.isEmpty()) {
            var player = store.getComponent(ref, Player.getComponentType());
            
            switch (data.action) {
                case "back" -> {
                    // Go back to scripts list
                    player.getPageManager().openCustomPage(ref, store, 
                        new ScriptsListUI(playerRef, CustomPageLifetime.CanDismiss));
                }
                case "save" -> {
                    // Save the script content
                    try {
                        ScriptLoader loader = SimpleScriptingPlugin.getInstance().getScriptLoader();
                        if (loader != null) {
                            var contextBuilder = loader.getContextBuilder();
                            if (contextBuilder != null) {
                                Path modsPath = contextBuilder.getModsPath();
                                Path fullPath = modsPath.resolve(scriptPath);
                                Files.writeString(fullPath, scriptContent);
                                playerRef.sendMessage(Message.raw("Script saved: " + scriptPath).color("#00FF00"));
                            }
                        }
                    } catch (IOException e) {
                        playerRef.sendMessage(Message.raw("Error saving script: " + e.getMessage()).color("#FF0000"));
                    }
                }
                case "reload" -> {
                    // Reload the script
                    try {
                        var scriptLoader = SimpleScriptingPlugin.getInstance().getScriptLoader();
                        scriptLoader.reloadScript(scriptPath);
                        playerRef.sendMessage(Message.raw("Script reloaded: " + scriptPath).color("#00FF00"));
                        
                        // Refresh content in case it changed
                        loadScriptContent();
                        sendUpdate();
                    } catch (Exception e) {
                        playerRef.sendMessage(Message.raw("Error reloading script: " + e.getMessage()).color("#FF0000"));
                    }
                }
            }
        }

        this.sendUpdate();
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
