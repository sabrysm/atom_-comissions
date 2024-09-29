package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.me.LootSplit.utils.Messages.*;

public class RemoveUserAmountCommand implements ISlashCommand {
    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("remove", "Removes a specified amount from a user’s balance.")
                .setGuildOnly(true)
                .addOption(OptionType.STRING, "username", "The player name", true)
                .addOption(OptionType.NUMBER, "amount", "The amount to remove", true);

    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply(false).queue();
        final String playerName = event.getOption("username").getAsString();
        final double amount = event.getOption("amount").getAsDouble();
        try {
            DatabaseManager databaseManager = new DatabaseManager();
            databaseManager.removeUserAmount(playerName, amount);
            sendAmountRemovedFromPlayer(event, playerName, amount);
        } catch (SQLException e) {
            raiseSQLError("Error giving amount to player: " + e.getMessage());
        } catch (Exception e) {
            raiseUnknownError("Error giving amount to player: " + e.getMessage());
        }
    }

}
