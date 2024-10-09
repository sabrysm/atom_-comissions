package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Objects;

import static com.me.LootSplit.utils.Messages.*;

public class RegisterCommand implements ISlashCommand {
    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("register", "Register your account with the in-game username")
                .setGuildOnly(true)
                .addOption(OptionType.STRING, "username", "Your in-game username", true)
                .addOption(OptionType.USER, "user", "The user to register", false);
    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        Dotenv config = Dotenv.configure().load();
        String allowedRole = config.get("ALLOWED_ROLE_FOR_LOOTSPLIT");


        try {
            event.deferReply(false).queue();
            String username = event.getOption("username").getAsString();
            Long userId = event.getOption("user") == null ? event.getUser().getIdLong() : Objects.requireNonNull(event.getOption("user")).getAsUser().getIdLong();
            DatabaseManager databaseManager = new DatabaseManager();

            // Only allow the command to be used by users with the specified role
            if (!event.getMember().getRoles().stream().anyMatch(r -> r.getName().equals(allowedRole)) && !event.getMember().getId().equals(userId.toString())) {
                sendPermissionDeniedMessage(event);
                return;
            }

            if (!databaseManager.isUserExists(username)) {
                sendUserNotInGuildListMessage(event, username);
                return;
            } else if (databaseManager.isUserRegistered(userId)) {
                sendUserAlreadyRegisteredMessage(event, event.getMember().getIdLong() == userId);
                return;
            } else {
                // Register the user in the database
                databaseManager.registerTheUser(username, userId);
                // Change the user's nickname to the in-game username
                event.getGuild().modifyNickname(event.getMember(), username).queue(
                        success -> event.getHook().sendMessage("Successfully changed nickname to " + username).queue(),
                        failure -> event.getHook().sendMessage("Failed to change nickname to " + username + " due to " + failure.getMessage()).queue()
                );
                sendUserRegisteredSuccessfullyMessage(event, username, event.getMember().getIdLong() == userId);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Could not register user due to Error: " + e.getMessage());

        } catch (InsufficientPermissionException e) {
            System.err.println("Could not register user due to Insufficient permissions: " + e.getMessage());

        } catch (HierarchyException e) {
            System.err.println("Could not register user due to Hierarchy issue: " + e.getMessage());

        } catch (Exception e) {
            raiseUnknownError("Could not register user " + e.getMessage());
        }
    }

    private void sendPermissionDeniedMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Permission Denied");
        embedBuilder.setDescription("You cannot register another user.");
        embedBuilder.setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

}
