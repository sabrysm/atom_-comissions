package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Objects;

import static com.me.LootSplit.utils.Messages.sendUserNotRegisteredMessage;

public class BalanceCommand implements ISlashCommand {
    Long userId;


    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("bal", "Check the balance of a player")
                .setGuildOnly(true)
                .addOption(OptionType.USER, "player", "The player to check the balance of", false);

    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        try {
            event.deferReply().queue();
            userId = event.getOption("player") == null ? null : Objects.requireNonNull(event.getOption("player")).getAsUser().getIdLong();
            boolean toCheckSelf = userId == null;
            if (userId == null) {
                userId = event.getUser().getIdLong();
            }
            DatabaseManager databaseManager = new DatabaseManager();
            if (!databaseManager.isUserRegistered(userId)) {
                sendUserNotRegisteredMessage(event, toCheckSelf);
                return;
            }
            String playerName = databaseManager.getPlayerName(userId);
            long balance = databaseManager.getPlayerBalance(userId);
            sendBalanceMessage(event, playerName, balance);
        } catch (SQLException e) {
            System.out.println("Error getting balance: " + e.getMessage());
        }
    }

    // Messages

    public static void sendBalanceMessage(@NotNull SlashCommandInteractionEvent event, String playerName, long balance) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        DecimalFormat formatter = new DecimalFormat("#,###");
        embedBuilder.setTitle(playerName + "'s Balance");
        embedBuilder.setDescription("Your current balance from \nall LootSplit sessions");
        embedBuilder.addField("\u200B", ":coin: " + formatter.format(balance), false);
        embedBuilder.setThumbnail(event.getUser().getAvatarUrl());
        embedBuilder.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }
}
