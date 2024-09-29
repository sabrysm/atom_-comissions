package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import com.me.LootSplit.utils.Paginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.me.LootSplit.utils.Messages.*;

public class LeaderboardCommand implements ISlashCommand {
    DecimalFormat formatter = new DecimalFormat("#,###");
    int numberOfPlayersToDisplay = 50;
    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("leaderboard", "Displays the leaderboard of the top players.")
                .setGuildOnly(true);
    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply(false).queue();
        try {
            DatabaseManager databaseManager = new DatabaseManager();
            OrderedMap<String, Double> topPlayers = databaseManager.getLeaderboard(numberOfPlayersToDisplay);
            sendLeaderboard(event, topPlayers);
        } catch (SQLException e) {
            raiseSQLError("Error getting leaderboard: " + e.getMessage());
        }
        catch (Exception e) {
            raiseUnknownError("Error getting leaderboard: " + e.getMessage());
        }
    }

    public void sendLeaderboard(SlashCommandInteractionEvent event, OrderedMap<String, Double> topPlayers) {
        List<MessageEmbed> leaderboardEmbeds = getPaginatorEmbed(topPlayers);
        Paginator leaderboardPaginator = new Paginator(leaderboardEmbeds, event.getUser().getIdLong());
        event.getJDA().addEventListener(leaderboardPaginator);
        leaderboardPaginator.sendInitialMessage(event);
    }

    public List<MessageEmbed> getPaginatorEmbed(OrderedMap<String, Double> players) {
        List<MessageEmbed> pages = new ArrayList<>();
        int usersPerPage = 10;
        int totalPages = (int) (Math.ceil((double) players.size() / usersPerPage));
        int currentPage = 0;
        int totalUsersNum = players.size();
        int counter = 0;
        List<String> playersNames = new ArrayList<>(players.keySet());
        for (int i = 0; i < players.size(); i += usersPerPage) {
            EmbedBuilder newPage = new EmbedBuilder();
            newPage.setTitle("Leaderboard");
            StringBuilder pageContent = new StringBuilder();
            pageContent.append("```").append(StringUtils.center("Top " + numberOfPlayersToDisplay + " Players", 50)).append("\n");
            for (int j = i; j < i + usersPerPage && j < totalUsersNum; j++) {
                counter++;
                pageContent.append(StringUtils.rightPad("#" + (j + 1) + " " + playersNames.get(j), 30)).append(StringUtils.center("", 10)).append(StringUtils.leftPad(formatter.format(players.get(playersNames.get(j))), 10)).append("\n");
            }
            pageContent.append("```");
            newPage.setDescription(pageContent.toString());
            newPage.setFooter("Page " + (currentPage + 1) + " of " + (totalPages));
            newPage.setColor(0x6064f4); // Blue
            pages.add(newPage.build());
            currentPage++;
        }
        System.out.println("Total number of users: " + counter);
        return pages;
    }

}
