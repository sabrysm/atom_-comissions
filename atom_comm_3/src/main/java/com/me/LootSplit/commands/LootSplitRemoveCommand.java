package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import com.me.LootSplit.utils.LootSplitSession;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.me.LootSplit.utils.Messages.*;

public class LootSplitRemoveCommand implements ISlashSubCommand {
    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        final String playerName = event.getOption("username").getAsString();
        final String splitId;
        DatabaseManager databaseManager;
        try {
            splitId = LootSplitSession.getLootSplitIdFromGuild(event.getGuild().getIdLong());
            databaseManager = new DatabaseManager();
            if (!databaseManager.isThereActiveSession(event.getGuild().getIdLong())) {
                sendNoActiveLootSplitSessionMessage(event);
                return;
            }
            if (!databaseManager.isUserExists(playerName)) {
                sendMessage("Not Found", "Player: " + playerName + " is not in the LootSplit Session", 0xFF0000, event);
                return;
            }
            databaseManager.removePlayerFromLootSplit(splitId, playerName);
            sendPlayerRemovedFromLootSplit(event, playerName);
        } catch (SQLException e) {
            raiseSQLError("Error removing player:"+ playerName +" from LootSplit: " + e.getMessage());
        } catch (Exception e) {
            raiseUnknownError("Error removing player:"+ playerName +" from LootSplit: " + e.getMessage());
        }
    }
}