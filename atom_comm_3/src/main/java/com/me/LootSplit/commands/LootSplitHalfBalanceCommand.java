package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import com.me.LootSplit.utils.LootSplitSession;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.me.LootSplit.utils.Messages.*;

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
        try {
            final String playerName = event.getOption("username").getAsString();
            final String splitId = LootSplitSession.getLootSplitIdFromGuild(event.getGuild().getIdLong());
             DatabaseManager databaseManager = new DatabaseManager();
             if (!databaseManager.isPlayerInLootSplit(splitId, playerName)) {
                 sendPlayerNotInLootSplitMessage(event, playerName);
                 return;
             }
             databaseManager.makePlayerBalanceHalvedInLootSplit(splitId, playerName);
             sendAmountHalvedFromPlayer(event, playerName);
        } catch (SQLException e) {
             raiseSQLError("Error halving amount from player: " + e.getMessage());
        }
        catch (Exception e) {
             raiseUnknownError("Error halving amount from player: " + e.getMessage());
        }
    }

}
