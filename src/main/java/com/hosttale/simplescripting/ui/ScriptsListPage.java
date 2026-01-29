package com.hosttale.simplescripting.ui;

import com.hosttale.simplescripting.scripts.ScriptBrowser;
import com.hosttale.simplescripting.scripts.ScriptBrowser.ScriptEntry;
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
import java.util.List;

public final class ScriptsListPage extends InteractiveCustomUIPage<ScriptsListPage.ScriptsListData> {

    private final ScriptBrowser browser;
    private final PlayerRef owner;
    private final String currentFolder;

    public ScriptsListPage(@Nonnull PlayerRef playerRef,
                           @Nonnull ScriptBrowser browser,
                           @Nonnull CustomPageLifetime lifetime) {
        this(playerRef, browser, lifetime, "");
    }

    public ScriptsListPage(@Nonnull PlayerRef playerRef,
                           @Nonnull ScriptBrowser browser,
                           @Nonnull CustomPageLifetime lifetime,
                           String folder) {
        super(playerRef, lifetime, ScriptsListData.CODEC);
        this.browser = browser;
        this.owner = playerRef;
        this.currentFolder = folder == null ? "" : folder;
    }

    public PlayerRef getOwner() {
        return owner;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder,
                      @Nonnull Store<EntityStore> store) {

        uiCommandBuilder.append("SimpleScripting/ScriptsList.ui");
        String title = currentFolder.isEmpty() ? "Scripts Manager" : "Scripts: /" + currentFolder;
        uiCommandBuilder.set("#Title.Text", title);

        List<ScriptEntry> entries = browser.list(currentFolder);
        int rowIndex = 0;

        if (!currentFolder.isEmpty()) {
            uiCommandBuilder.append("#ScriptsList", "SimpleScripting/ScriptsEntry.ui");
            uiCommandBuilder.set("#ScriptsList[" + rowIndex + "] #EntryLabel.Text", "<- Back");
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#ScriptsList[" + rowIndex + "] #EntryButton",
                    EventData.of(ScriptsListData.KEY_ACTION, "back"));
            rowIndex++;
        }

        for (ScriptEntry entry : entries) {
            uiCommandBuilder.append("#ScriptsList", "SimpleScripting/ScriptsEntry.ui");
            String icon = entry.isFolder() ? "[D]" : "[F]";
            String displayName = entry.isFolder() ? entry.name() + "/" : entry.name();
            uiCommandBuilder.set("#ScriptsList[" + rowIndex + "] #EntryLabel.Text",
                    icon + " " + escape(displayName));

            String action = (entry.isFolder() ? "folder:" : "script:") + entry.path();
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#ScriptsList[" + rowIndex + "] #EntryButton",
                    EventData.of(ScriptsListData.KEY_ACTION, action));
            rowIndex++;
        }

        uiCommandBuilder.append("#ActionButtons", "SimpleScripting/ScriptsReloadButton.ui");
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#ActionButtons[0] #ReloadAllBtn",
                EventData.of(ScriptsListData.KEY_ACTION, "reload_all"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull ScriptsListData data) {
        super.handleDataEvent(ref, store, data);

        if (data.action == null || data.action.isBlank()) {
            sendUpdate();
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef targetRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || targetRef == null) {
            return;
        }

        String action = data.action;
        if (action.equals("back")) {
            player.getPageManager().openCustomPage(ref, store,
                    new ScriptsListPage(targetRef, browser, CustomPageLifetime.CanDismiss, parentFolder(currentFolder)));
            return;
        }

        if (action.equals("reload_all")) {
            int count = browser.reloadAllMods();
            targetRef.sendMessage(Message.raw("Reloaded " + count + " mods.").color("#00ff99"));
            player.getPageManager().openCustomPage(ref, store,
                    new ScriptsListPage(targetRef, browser, CustomPageLifetime.CanDismiss, currentFolder));
            return;
        }

        if (action.startsWith("folder:")) {
            String folder = action.substring("folder:".length());
            player.getPageManager().openCustomPage(ref, store,
                    new ScriptsListPage(targetRef, browser, CustomPageLifetime.CanDismiss, folder));
            return;
        }

        if (action.startsWith("script:")) {
            String scriptPath = action.substring("script:".length());
            player.getPageManager().openCustomPage(ref, store,
                    new ScriptViewerPage(targetRef, browser, scriptPath));
            return;
        }

        sendUpdate();
    }

    private String parentFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "";
        }
        int lastSlash = folder.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "";
        }
        return folder.substring(0, lastSlash);
    }

    private String escape(String value) {
        return value.replace("\"", "\\\"").replace("\n", " ");
    }

    public static class ScriptsListData {
        static final String KEY_ACTION = "Action";

        public static final BuilderCodec<ScriptsListData> CODEC =
                BuilderCodec.<ScriptsListData>builder(ScriptsListData.class, ScriptsListData::new)
                        .addField(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                                (data, value) -> data.action = value,
                                data -> data.action)
                        .build();

        private String action;
    }
}
