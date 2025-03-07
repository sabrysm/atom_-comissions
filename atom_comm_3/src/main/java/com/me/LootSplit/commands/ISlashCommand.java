package com.me.LootSplit.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public interface ISlashCommand {
    // Runs the slash command, only called if the command name matches
    void execute(@NotNull SlashCommandInteractionEvent event);

    // Returns the object describing the slash command
    @NotNull
    CommandData getCommandData();
}