package com.hosttale.simplescripting.commands;

import com.hosttale.simplescripting.mod.ModTemplateService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

public final class CreateModCommand extends CommandBase {

    private final ModTemplateService templateService;
    private final RequiredArg<String> nameArg;

    public CreateModCommand(ModTemplateService templateService) {
        super("createmod", "Create a JS mod from the template.");
        this.templateService = templateService;
        this.nameArg = withRequiredArg("mod-name", "Mod identifier (lowercase, digits, -, _)", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(CommandContext commandContext) {
        String modName = commandContext.get(nameArg);
        templateService.createMod(modName, commandContext);
    }
}
