package com.me.GuildBot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

public interface ISlashSubCommand {
    // Runs the slash command, only called if the command name matches
    void execute(@NotNull SlashCommandInteractionEvent event);
}
