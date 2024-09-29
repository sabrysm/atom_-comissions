package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import com.me.LootSplit.utils.LootSplitSession;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.me.LootSplit.utils.Messages.*;

public class LootSplitAddCommand implements ISlashSubCommand {
    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        final String playerName = event.getOption("username").getAsString();
        final String splitId;
        final String uploadId;
        DatabaseManager databaseManager;
        try {
            splitId = LootSplitSession.getLootSplitIdFromGuild(event.getGuild().getIdLong());
            uploadId = LootSplitSession.generateId("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
            databaseManager = new DatabaseManager();
            if (!databaseManager.isThereActiveSession(event.getGuild().getIdLong())) {
                sendNoActiveLootSplitSessionMessage(event);
                return;
            }
            if (!databaseManager.isUserExists(playerName)) {
                sendMessage("Denied Operation", "Player: " + playerName + " is not in the Guild List!\nPlease add the player to the Guild List first.", 0xFF0000, event);
                return;
            }
            databaseManager.addPlayerToLootSplit(splitId, uploadId, playerName, event.getGuild().getIdLong());
            sendPlayerAddedToLootSplit(event, playerName);
        } catch (SQLException e) {
            raiseSQLError("Error adding player:"+ playerName +" to LootSplit: " + e.getMessage());
        } catch (Exception e) {
            raiseUnknownError("Error adding player:"+ playerName +" to LootSplit: " + e.getMessage());
        }
    }
}