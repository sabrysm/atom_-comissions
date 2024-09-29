package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class GuildRemoveCommand implements ISlashSubCommand {
    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        try {
            final String guildName = event.getOption("guild_name").getAsString();
            final DatabaseManager databaseManager = new DatabaseManager();
            databaseManager.removeGuildList(guildName, event.getGuild().getIdLong());
            sendSuccessRemoveMessage(event);
        } catch (SQLException e) {
            sendErrorRemoveMessage(event, "SQL");
        } catch (Exception e) {
            sendErrorRemoveMessage(event, "Unknown");
        }
    }

    // Messages
    private void sendErrorRemoveMessage(@NotNull SlashCommandInteractionEvent event, String errorType) {
        final EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(errorType + " Error");
        embedBuilder.setDescription("There was an error removing the guild list.");
        embedBuilder.setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    private void sendSuccessRemoveMessage(@NotNull SlashCommandInteractionEvent event) {
        final EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Success");
        embedBuilder.setDescription("The guild list has been removed.");
        embedBuilder.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }
}
