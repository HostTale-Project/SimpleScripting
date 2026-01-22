package com.hosttale.simplescripting.commands;

import com.hosttale.simplescripting.mod.ModTemplateService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateModCommandTest {

    @Test
    void executeSyncDelegatesToTemplateService() {
        ModTemplateService templateService = mock(ModTemplateService.class);
        CommandContext ctx = mock(CommandContext.class);
        CreateModCommand command = new CreateModCommand(templateService);
        when(ctx.get(ArgumentMatchers.any())).thenReturn("my-mod");

        command.executeSync(ctx);

        verify(templateService).createMod("my-mod", ctx);
    }
}
