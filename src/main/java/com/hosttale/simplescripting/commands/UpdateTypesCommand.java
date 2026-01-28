package com.hosttale.simplescripting.commands;

import com.hosttale.simplescripting.mod.ModTemplateService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

public final class UpdateTypesCommand extends CommandBase {

    private final ModTemplateService templateService;
    private final RequiredArg<String> nameArg;

    public UpdateTypesCommand(ModTemplateService templateService) {
        super("updatetypes", "Refresh index.d.ts for a mod.");
        this.templateService = templateService;
        this.nameArg = withRequiredArg("mod-name", "Target mod identifier", ArgTypes.STRING);
        requirePermission("simplescripting.commands.updatetypes");
    }

    @Override
    protected void executeSync(CommandContext commandContext) {
        String modName = commandContext.get(nameArg);
        templateService.updateTypes(modName, commandContext);
    }
}
