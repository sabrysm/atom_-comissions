package com.me.LootSplit.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class LootSplitHalfBalanceCommand implements ISlashCommand {
    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("half", "Halves the balance of a player.")
                .setGuildOnly(true)
                .addOption(OptionType.STRING, "username", "The player name", true);

    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
    }

}
