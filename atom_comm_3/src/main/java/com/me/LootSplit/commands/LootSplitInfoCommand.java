package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import com.me.LootSplit.utils.LootSplitSubmissionPaginator;
import com.me.LootSplit.utils.Paginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.collections4.OrderedMap;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.me.LootSplit.utils.Messages.*;

public class LootSplitInfoCommand implements ISlashSubCommand {
    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        try {
            String splitId = event.getOption("split_id").getAsString();
            long guildId = event.getGuild().getIdLong();
            System.out.println("Guild ID: " + guildId);
            DatabaseManager databaseManager = new DatabaseManager();
            if (!databaseManager.isThereActiveSession(guildId)) {
                sendNoActiveLootSplitSessionMessage(event);
                return;
            }
            Integer silver, items;
            List<Integer> silverAndItems = databaseManager.getLootSplitSilverAndItems(splitId);
            silver = silverAndItems.get(0);
            items = silverAndItems.get(1);
            double amountToSplit = (silver + items)*0.7 / databaseManager.getLootSplitPlayersCount(splitId, guildId);
            // Get LootSplit info
            OrderedMap<String, Object> lootSplitInfo = databaseManager.getLootSplitSessionInfo(splitId);

            sendLootSplitInfo(event, lootSplitInfo, amountToSplit);
        } catch (SQLException e) {
            raiseSQLError("Error wit LootSplit info: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            raiseUnknownError("Error wit LootSplit info: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendLootSplitInfo(SlashCommandInteractionEvent event, OrderedMap<String, Object> lootSplitInfo, double amount) throws SQLException {
        List<MessageEmbed> pages = getPaginatorEmbed(event, lootSplitInfo, amount);
        long userId = event.getInteraction().getUser().getIdLong();
        Paginator paginator = new Paginator(pages, userId);
        event.getJDA().addEventListener(paginator);
        paginator.sendInitialMessage(event);
    }

    public static void main(String[] args) {
        double number = 1.23e7;  // Example number in scientific notation

        // Define the format pattern with commas
        DecimalFormat formatter = new DecimalFormat("#,###");

        // Format the number
        String formattedNumber = formatter.format(number);

        // Output the result
        System.out.println(formattedNumber);  // Outputs: 12,300,000
    }

    public static List<MessageEmbed> getPaginatorEmbed(SlashCommandInteractionEvent event, OrderedMap<String, Object> lootSplitInfo, double amount) throws SQLException {
        List<MessageEmbed> pages = new ArrayList<>();
        int usersPerPage = 10;
        EmbedBuilder firstPage = new EmbedBuilder();
        DatabaseManager databaseManager = new DatabaseManager();
        DecimalFormat formatter = new DecimalFormat("#,###");
        String splitId = lootSplitInfo.get("split_id").toString();
        long guildId = event.getGuild().getIdLong();
        Integer totalPlayers = databaseManager.getLootSplitPlayersCount(splitId, guildId);
        Integer Silver = (Integer) lootSplitInfo.get("silver");
        Integer Items = (Integer) lootSplitInfo.get("items");
        Double guildSplitFee = amount * 0.3;
        String partyLeader = lootSplitInfo.get("session_creator").toString();
        List<String> halfSplitPlayers = databaseManager.getHalfSplitPlayers(splitId);
        List<String> fullSplitPlayers = databaseManager.getFullSplitPlayers(splitId);
        List<String> players = new ArrayList<>();
        players.addAll(halfSplitPlayers);
        players.addAll(fullSplitPlayers);
        players.sort(String::compareToIgnoreCase);
        firstPage.setTitle("Loot Split Info");
        firstPage.addField("Total Value", formatter.format(Silver + Items), true);
        firstPage.addField("Silver Value", formatter.format(Silver), true);
        firstPage.addField("Loot Value", formatter.format(Items), true);
        firstPage.addField("Member Count", totalPlayers.toString(), true);
        // Empty Field
        firstPage.addField("\u200B", "\u200B", true);
        firstPage.addField("Guild Split Fee", formatter.format(guildSplitFee), true);
        firstPage.addField("Payout per Player", formatter.format(amount), true);
        // Empty Field
        firstPage.addField("\u200B", "\u200B", true);
        firstPage.addField("Payout per Player (Half)", formatter.format(amount / 2), true);
        firstPage.addField("Party Leader", partyLeader, false);
        firstPage.setColor(0x6064f4); // Blue
        int totalPages = (int) (Math.ceil((double) players.size() / usersPerPage)) + 1;
        int currentPage = 0;
        int totalUsersNum = players.size();
        int counter = 0;
        firstPage.setFooter("Page " + (currentPage + 1) + " of " + totalPages);
        currentPage++;
        pages.add(firstPage.build());
        for (int i = 0; i < players.size(); i += usersPerPage) {
            EmbedBuilder newPage = new EmbedBuilder();
            newPage.setTitle("Members in Loot Split");
            StringBuilder pageContent = new StringBuilder();
            String header = " **Player Name**\n";
            for (int j = i; j < i + usersPerPage && j < totalUsersNum; j++) {
                counter++;
                String line = "- **" + players.get(j) + "**\n";
                pageContent.append(line);
            }
            newPage.setDescription("Total number of users added: **" + totalUsersNum + "**\n\n" + header + pageContent.toString());
            newPage.setFooter("Page " + (currentPage + 1) + " of " + (totalPages));
            newPage.setColor(0x6064f4); // Blue
            pages.add(newPage.build());
            currentPage++;
        }
        System.out.println("Total number of users: " + counter);
        return pages;
    }
}