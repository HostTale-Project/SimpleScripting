package com.hosttale.simplescripting.mod.runtime.api;

import com.hosttale.simplescripting.mod.runtime.api.assets.AssetsApi;
import com.hosttale.simplescripting.mod.runtime.api.commands.CommandsApi;
import com.hosttale.simplescripting.mod.runtime.api.events.EventsApi;
import com.hosttale.simplescripting.mod.runtime.api.modules.ModuleImports;
import com.hosttale.simplescripting.mod.runtime.api.net.NetApi;
import com.hosttale.simplescripting.mod.runtime.api.players.PlayersApi;
import com.hosttale.simplescripting.mod.runtime.api.server.ServerApi;
import com.hosttale.simplescripting.mod.runtime.api.ui.UiApi;
import com.hosttale.simplescripting.mod.runtime.api.worlds.WorldsApi;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WrapperConventionsTest {

    private static final Set<Class<?>> API_CLASSES = Set.of(
            EventsApi.class,
            CommandsApi.class,
            PlayersApi.class,
            WorldsApi.class,
            ServerApi.class,
            NetApi.class,
            AssetsApi.class,
            UiApi.class,
            ModuleImports.class
    );

    @Test
    void publicMethodsUseCamelCase() {
        API_CLASSES.forEach(clazz -> {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                if (method.isSynthetic() || method.getName().contains("$")) {
                    continue;
                }
                assertTrue(method.getName().matches("^[a-z][A-Za-z0-9]*$"),
                        () -> clazz.getSimpleName() + "." + method.getName() + " should be camelCase");
            }
        });
    }
}
