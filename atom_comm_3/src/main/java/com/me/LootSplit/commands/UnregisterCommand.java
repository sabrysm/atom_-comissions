package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Objects;

import static com.me.LootSplit.utils.Messages.*;

public class UnregisterCommand implements ISlashCommand {
    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("unregister", "Unregister your account with the in-game username")
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
            } else if (!databaseManager.isUserRegistered(userId)) {
                sendUserNotRegisteredMessage(event, event.getMember().getIdLong() == userId);
                return;
            } else {
                // Unregister the user in the database
                databaseManager.unregisterTheUser(username, userId);
                // Remove nickname
                event.getGuild().retrieveMemberById(userId).queue(member -> {
                    event.getGuild().modifyNickname(member, null).queue();
                });
                sendUserUnregisteredSuccessfullyMessage(event, username, event.getMember().getIdLong() == userId);
            }
        } catch (SQLException e) {
            raiseSQLError("Could not unregister user");
        } catch (Exception e) {
            event.getHook().sendMessage("Could not unregister user").queue();
            raiseUnknownError("Could not unregister user");
        }
    }

    private void sendPermissionDeniedMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Permission Denied");
        embedBuilder.setDescription("You cannot unregister another user.");
        embedBuilder.setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

}