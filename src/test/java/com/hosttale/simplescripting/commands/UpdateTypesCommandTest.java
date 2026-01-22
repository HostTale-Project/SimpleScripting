package com.hosttale.simplescripting.commands;

import com.hosttale.simplescripting.mod.ModTemplateService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateTypesCommandTest {

    @Test
    void executeSyncDelegatesToTemplateService() {
        ModTemplateService templateService = mock(ModTemplateService.class);
        CommandContext ctx = mock(CommandContext.class);
        when(ctx.get(any())).thenReturn("my-mod");

        UpdateTypesCommand command = new UpdateTypesCommand(templateService);

        command.executeSync(ctx);

        verify(templateService).updateTypes("my-mod", ctx);
    }
}
