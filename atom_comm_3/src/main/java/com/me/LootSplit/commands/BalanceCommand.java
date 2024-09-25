package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class BalanceCommand implements ISlashCommand {
    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("bal", "Check your balance")
                .setGuildOnly(true);
    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        try {
            long userId = Long.parseLong(event.getUser().getId());
            DatabaseManager databaseManager = new DatabaseManager();
            long balance = databaseManager.getPlayerBalance(userId);
            sendBalanceMessage(event, balance);
        } catch (SQLException e) {
            System.out.println("Error getting balance: " + e.getMessage());
        }
    }

    // Messages

    public static void sendBalanceMessage(@NotNull SlashCommandInteractionEvent event, long balance) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Balance");
        embedBuilder.setDescription("Your balance is: " + balance);
        embedBuilder.setColor(0x00FF00);
        event.replyEmbeds(embedBuilder.build()).queue();
    }
}
