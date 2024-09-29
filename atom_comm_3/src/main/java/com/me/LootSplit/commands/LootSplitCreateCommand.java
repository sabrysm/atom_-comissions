package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import com.me.LootSplit.utils.LootSplitSession;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.me.LootSplit.utils.Messages.*;

public class LootSplitCreateCommand implements ISlashSubCommand {
    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getOption("name").getAsString();
        LootSplitSession lootSplitSession;
        try {
            DatabaseManager databaseManager = new DatabaseManager();
            if (databaseManager.isThereActiveSession(Long.parseLong(event.getGuild().getId()))) {
                sendLootSplitSessionAlreadyActiveMessage(event);
                return;
            }
            if (!databaseManager.isUserRegistered(event.getUser().getIdLong())) {
                sendUserNotRegisteredMessage(event, true);
                return;
            }
            lootSplitSession = new LootSplitSession(name, Long.parseLong(event.getGuild().getId()));
            String splitId = lootSplitSession.getId();
            databaseManager.createNewLootSplitSession(splitId, name, Long.parseLong(event.getGuild().getId()), event.getUser().getName());
            sendLootSplitSessionCreatedMessage(event, name, splitId);
        } catch (SQLException e) {
            raiseSQLError("Error creating LootSplit session");
            return;
        } catch (Exception e) {
            raiseUnknownError("Error creating LootSplit session");
            return;
        }
    }
}
