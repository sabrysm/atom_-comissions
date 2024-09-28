package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

import static com.me.LootSplit.utils.Messages.*;

public class LootSplitConfirmCommand implements ISlashSubCommand {
    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        try {
            String splitId = event.getOption("split_id").getAsString();
            DatabaseManager databaseManager = new DatabaseManager();
            if (!databaseManager.isThereActiveSession(event.getGuild().getIdLong())) {
                sendNoActiveLootSplitSessionMessage(event);
                return;
            }
            Integer silver, items;
            List<Integer> silverAndItems = databaseManager.getLootSplitSilverAndItems(splitId);
            silver = silverAndItems.get(0);
            items = silverAndItems.get(1);
            double amountToSplit = (silver + items)*0.7 / databaseManager.getLootSplitPlayersCount(splitId, event.getGuild().getIdLong());
            databaseManager.addBalanceToAllLootSplitPlayers(splitId, amountToSplit);
            sendAmountAddedToAllPlayers(event, amountToSplit);
        } catch (SQLException e) {
            raiseSQLError("Error confirming LootSplit: " + e.getMessage());
        } catch (Exception e) {
            raiseUnknownError("Error confirming LootSplit: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        double number = 1.23e7;  // Example number in scientific notation

        // Define the format pattern with commas
        DecimalFormat formatter = new DecimalFormat("#,###");

        // Format the number
        String formattedNumber = formatter.format(number);

        // Output the result
        System.out.println(formattedNumber);  // Outputs: 12,300,000
    }
}