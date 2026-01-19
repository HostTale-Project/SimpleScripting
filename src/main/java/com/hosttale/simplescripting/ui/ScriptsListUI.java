package com.hosttale.simplescripting.ui;

import com.hosttale.simplescripting.SimpleScriptingPlugin;
import com.hosttale.simplescripting.script.ScriptLoader;
import com.hosttale.simplescripting.util.MessageHelper;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Interactive UI page that displays a list of scripts and allows re-executing them.
 * Supports folder navigation - clicking a folder shows its contents.
 */
public class ScriptsListUI extends InteractiveCustomUIPage<ScriptsListUI.ScriptsListData> {
    
    // Current folder being viewed (empty string = root mods folder)
    private String currentFolder;
    
    public ScriptsListUI(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
        super(playerRef, lifetime, ScriptsListData.CODEC);
        this.currentFolder = "";
    }
    
    public ScriptsListUI(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String folder) {
        super(playerRef, lifetime, ScriptsListData.CODEC);
        this.currentFolder = folder != null ? folder : "";
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                      @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        
        uiCommandBuilder.append("SimpleScripting/ScriptsList.ui");
        
        // Set title based on current folder
        String title = currentFolder.isEmpty() ? "Scripts Manager" : "Scripts: /" + currentFolder;
        uiCommandBuilder.set("#Title.Text", title);
        
        // Get list of entries (folders and scripts)
        List<ScriptEntry> entries = getScriptEntries();
        
        int rowIndex = 0;
        
        // Add back button if we're in a subfolder
        if (!currentFolder.isEmpty()) {
            uiCommandBuilder.append("#ScriptsList", "SimpleScripting/ScriptsEntry.ui");
            uiCommandBuilder.set("#ScriptsList[" + rowIndex + "] #EntryLabel.Text", "<- Back");
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#ScriptsList[" + rowIndex + "] #EntryButton", 
                EventData.of("Action", "back"));
            rowIndex++;
        }
        
        // Add entries
        for (int i = 0; i < entries.size(); i++) {
            ScriptEntry entry = entries.get(i);

            // Create row for each entry
            String icon = entry.isFolder ? "[D]" : "[F]";
            String displayName = entry.isFolder ? entry.name + "/" : entry.name;

            uiCommandBuilder.append("#ScriptsList", "SimpleScripting/ScriptsEntry.ui");
            uiCommandBuilder.set("#ScriptsList[" + rowIndex + "] #EntryLabel.Text", icon + " " + escapeString(displayName));

            // Bind click event
            String actionValue = entry.isFolder ? "folder:" + entry.path : "script:" + entry.path;
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#ScriptsList[" + rowIndex + "] #EntryButton",
                EventData.of("Action", actionValue));

            rowIndex++;
        }
        
        // Add reload all button at the bottom
        uiCommandBuilder.append("#ActionButtons", "SimpleScripting/ScriptsReloadButton.ui");
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
            "#ActionButtons[0] #ReloadAllBtn", 
            EventData.of("Action", "reload_all"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                                @Nonnull ScriptsListData data) {
        super.handleDataEvent(ref, store, data);
        
        if (data.action != null && !data.action.isEmpty()) {
            var player = store.getComponent(ref, Player.getComponentType());
            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            
            if (data.action.equals("back")) {
                // Navigate to parent folder
                String parentFolder = getParentFolder(currentFolder);
                player.getPageManager().openCustomPage(ref, store, 
                    new ScriptsListUI(playerRef, CustomPageLifetime.CanDismiss, parentFolder));
                return;
            }
            
            if (data.action.equals("reload_all")) {
                // Reload all scripts
                int count = SimpleScriptingPlugin.getInstance().reloadScripts();
                player.sendMessage(MessageHelper.raw("§aReloaded " + count + " scripts!"));
                // Refresh the UI
                player.getPageManager().openCustomPage(ref, store, 
                    new ScriptsListUI(playerRef, CustomPageLifetime.CanDismiss, currentFolder));
                return;
            }
            
            if (data.action.startsWith("folder:")) {
                // Navigate into folder
                String folder = data.action.substring(7);
                player.getPageManager().openCustomPage(ref, store, 
                    new ScriptsListUI(playerRef, CustomPageLifetime.CanDismiss, folder));
                return;
            }
            
            if (data.action.startsWith("script:")) {
                // Open script viewer to see content
                String scriptPath = data.action.substring(7);
                player.getPageManager().openCustomPage(ref, store, 
                    new ScriptViewerUI(playerRef, scriptPath));
                return;
            }
        }
        
        sendUpdate();
    }
    
    /**
     * Gets the parent folder path.
     */
    private String getParentFolder(String folder) {
        if (folder == null || folder.isEmpty()) {
            return "";
        }
        int lastSlash = folder.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "";
        }
        return folder.substring(0, lastSlash);
    }
    
    /**
     * Gets the list of script entries (folders and files) for the current folder.
     */
    private List<ScriptEntry> getScriptEntries() {
        List<ScriptEntry> entries = new ArrayList<>();
        
        try {
            SimpleScriptingPlugin plugin = SimpleScriptingPlugin.getInstance();
            if (plugin == null || plugin.getScriptLoader() == null) {
                return entries;
            }
            
            ScriptLoader loader = plugin.getScriptLoader();
            Path modsPath = getModsPath();
            
            if (modsPath == null || !Files.exists(modsPath)) {
                return entries;
            }
            
            Path currentPath = currentFolder.isEmpty() ? modsPath : modsPath.resolve(currentFolder);
            
            if (!Files.exists(currentPath) || !Files.isDirectory(currentPath)) {
                return entries;
            }
            
            List<ScriptEntry> folders = new ArrayList<>();
            List<ScriptEntry> scripts = new ArrayList<>();
            
            try (Stream<Path> stream = Files.list(currentPath)) {
                stream.forEach(path -> {
                    String name = path.getFileName().toString();
                    String relativePath = modsPath.relativize(path).toString();
                    
                    if (Files.isDirectory(path)) {
                        folders.add(new ScriptEntry(name, relativePath, true));
                    } else if (name.endsWith(".js")) {
                        scripts.add(new ScriptEntry(name, relativePath, false));
                    }
                });
            }
            
            // Sort alphabetically
            folders.sort(Comparator.comparing(e -> e.name.toLowerCase()));
            scripts.sort(Comparator.comparing(e -> e.name.toLowerCase()));
            
            // Folders first, then scripts
            entries.addAll(folders);
            entries.addAll(scripts);
            
        } catch (IOException e) {
            // Log error but return empty list
            SimpleScriptingPlugin.getInstance().getLogger().atWarning()
                .log("Error listing scripts: " + e.getMessage());
        }
        
        return entries;
    }
    
    /**
     * Gets the mods path from the plugin.
     */
    private Path getModsPath() {
        try {
            // Access via reflection since ModsDirectoryManager is package-private
            ScriptLoader loader = SimpleScriptingPlugin.getInstance().getScriptLoader();
            if (loader != null) {
                var contextBuilder = loader.getContextBuilder();
                if (contextBuilder != null) {
                    return contextBuilder.getModsPath();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Escapes a string for use in UI text.
     */
    private String escapeString(String str) {
        return str.replace("\"", "\\\"").replace("\n", " ");
    }
    
    /**
     * Represents an entry in the scripts list (file or folder).
     */
    private static class ScriptEntry {
        final String name;
        final String path;
        final boolean isFolder;
        
        ScriptEntry(String name, String path, boolean isFolder) {
            this.name = name;
            this.path = path;
            this.isFolder = isFolder;
        }
    }
    
    /**
     * Data class for UI events.
     */
    public static class ScriptsListData {
        static final String KEY_ACTION = "Action";
        
        public static final BuilderCodec<ScriptsListData> CODEC = 
            BuilderCodec.<ScriptsListData>builder(ScriptsListData.class, ScriptsListData::new)
                    .addField(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                    (data, s) -> data.action = s, 
                    data -> data.action)
                .build();
        
        private String action;
    }
}
