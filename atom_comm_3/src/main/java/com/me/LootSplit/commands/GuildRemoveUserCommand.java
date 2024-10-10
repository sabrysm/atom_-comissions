package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class GuildRemoveUserCommand implements ISlashSubCommand {
    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        try {
            final String userName = event.getOption("user").getAsString();
            final DatabaseManager databaseManager = new DatabaseManager();
            databaseManager.removeGuildUser(event.getGuild().getIdLong(), userName);
            sendSuccessRemoveUserMessage(event, userName);
        } catch (SQLException e) {
            System.err.println("An SQL Exception occurred while removing the user from the guild list: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An Exception occurred while removing the user from the guild list: " + e.getMessage());
            sendErrorRemoveUserMessage(event, event.getOption("user").getAsString());
        }
    }


    private void sendSuccessRemoveUserMessage(@NotNull SlashCommandInteractionEvent event, String user) {
        final EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Success");
        embedBuilder.setDescription("The user " + user + " has been removed from the guild list.");
        embedBuilder.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    private void sendErrorRemoveUserMessage(@NotNull SlashCommandInteractionEvent event, String user) {
        final EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Error");
        embedBuilder.setDescription("There was an error removing the user " + user + " from the guild list.");
        embedBuilder.setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    private void sendSuccessAddUserMessage(@NotNull SlashCommandInteractionEvent event, String user) {
        final EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Success");
        embedBuilder.setDescription("The user " + user + " has been added to the guild list.");
        embedBuilder.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    private void sendErrorAddUserMessage(@NotNull SlashCommandInteractionEvent event, String user) {
        final EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Error");
        embedBuilder.setDescription("There was an error adding the user " + user + " to the guild list.");
        embedBuilder.setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }
}
