package com.me.GuildBot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

public interface ISlashCommand {
    // Runs the slash command, only called if the command name matches
    void execute(@NotNull SlashCommandInteractionEvent event);

    // Returns the object describing the slash command
    @NotNull
    CommandData getCommandData();
}