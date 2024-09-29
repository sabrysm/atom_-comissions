package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import com.me.LootSplit.utils.LootSplitSession;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.SQLException;

import static com.me.LootSplit.utils.Messages.*;

public class LootSplitSetCommand implements ISlashSubCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        try {
            Integer silver = event.getOption("silver").getAsInt();
            Integer items = event.getOption("items").getAsInt();
            DatabaseManager databaseManager = new DatabaseManager();
            if (!databaseManager.isThereActiveSession(event.getGuild().getIdLong())) {
                sendNoActiveLootSplitSessionMessage(event);
                return;
            }
            String splitId = LootSplitSession.getLootSplitIdFromGuild(event.getGuild().getIdLong());
            databaseManager.setLootSplitSilverAndItems(splitId, silver, items);
            sendMessage("LootSplit Set", "LootSplit set to " + silver + " silver and " + items + " item value", 0x6064f4, event);
        } catch (SQLException e) {
            raiseSQLError("Error setting LootSplit: " + e.getMessage());
        } catch (Exception e) {
            raiseUnknownError("Error setting LootSplit: " + e.getMessage());
        }
    }
}