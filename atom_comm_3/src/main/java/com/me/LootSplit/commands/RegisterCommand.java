package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.me.LootSplit.utils.Messages.*;

public class RegisterCommand implements ISlashCommand {
    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("register", "Register your account with the in-game username")
                .setGuildOnly(true)
                .addOption(OptionType.STRING, "username", "Your in-game username", true);
    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        try {
            event.deferReply(false).queue();
            String username = event.getOption("username").getAsString();
            DatabaseManager databaseManager = new DatabaseManager();

            if (!databaseManager.isUserExists(username)) {
                sendUserNotInGuildListMessage(event, username);
                return;
            } else if (databaseManager.isUserRegistered(Long.parseLong(event.getUser().getId()))) {
                sendUserAlreadyRegisteredMessage(event);
                return;
            } else {
                // Register the user in the database
                databaseManager.registerTheUser(username, Long.parseLong(event.getUser().getId()));
                // Change the user's nickname to the in-game username
                event.getGuild().modifyNickname(event.getMember(), username).queue();
                sendUserRegisteredSuccessfullyMessage(event, username);
            }
        } catch (SQLException e) {
            raiseSQLError("Could not register user");
        } catch (Exception e) {
            event.getHook().sendMessage("Could not register user").queue();
            raiseUnknownError("Could not register user");
        }
    }

}
