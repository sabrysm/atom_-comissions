package com.me.LootSplit.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class GiveUserAmountCommand implements ISlashCommand {
    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("give", "Give a specified amount to a user’s balance.")
                .setGuildOnly(true)
                .addOption(OptionType.STRING, "username", "The player name", true)
                .addOption(OptionType.INTEGER, "amount", "The amount to give", true);
    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
    }

}
