package com.me.LootSplit.utils;

import com.me.LootSplit.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.xml.crypto.Data;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LootSplitGuildUploadHelper {
    /**
     * Converts a CSV string to a 2D array
     *
     * @param text The CSV string
     * @return The 2D array
     */

    private final List<List<String>> roles = new ArrayList<>();
    private final List<String> players = new ArrayList<>();
    private final long guildID;
    private final String splitID;
    private final String uploadID;
    private final boolean isFile;

    public LootSplitGuildUploadHelper(String text, String splitID, long guildID, boolean isFile) throws Exception {
        this.guildID = guildID;
        this.splitID = splitID;
        this.uploadID = generateUploadId();
        this.isFile = isFile;
        parseText(text);
    }


    public void uploadGuildUsers() throws SQLException {
        // Add the users to the database
        addUsersToDatabase();
    }

    private String generateUploadId() {
        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder builder = new StringBuilder();
        int count = 7;
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    public void sendGuildUploadPaginator(SlashCommandInteractionEvent event) {
        List<MessageEmbed> guildEmbeds = getPaginatorEmbed();
        Paginator guildPaginator = new Paginator(guildEmbeds, event.getUser().getIdLong());
        event.getJDA().addEventListener(guildPaginator);
        guildPaginator.sendInitialMessage(event);
    }

    public void parseText(String text) throws Exception {
        try {
            String[] lines;
            // Split the text by new line
            if (isFile) {
                lines = text.split("\n");
            } else {
                // "868PABLOW","08/14/2024 15:19:43","" "ADESUWU","08/14/2024 15:19:32","" I want " " to be the delimiter
                lines = text.split("\" \"");
                // System.out.println("Lines length: " + lines.length);
            }

            int totalNumOfUsers = lines.length;
            // Create a 2D array to store the results
            String[][] result = new String[totalNumOfUsers][];

            // Process each line
            // i=1 to skip the first line which contains the column names
            for (int i = 0; i < lines.length; i++) {
                // Remove surrounding quotes and split by ","
                if (isFile) {
                    result[i] = lines[i].replaceAll("^\"|\"$", "").split("\"\t\"");
                } else {
                    // remove any quote
                    result[i] = lines[i].trim().replaceAll("^\"|\"$", "").trim().split(" {4}");
                    for (int j = 0; j < result[i].length; j++) {
                        result[i][j] = result[i][j].replaceAll("\"", "");
                    }
//                    System.out.println("Result: " + Arrays.toString(result[i]));
                }
            }

            // Process each line to get the usernames
            for (int i = 1; i < result.length; i++) {
                for (int j = 0; j < result[1].length; j++) {
                    if (j == 0) {
                        players.add(result[i][j]);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing the text: " + e.getMessage());
        }
    }

    public List<MessageEmbed> getPaginatorEmbed() {
        List<MessageEmbed> pages = new ArrayList<>();
        try {
            DatabaseManager manager = new DatabaseManager();
            List<String> registeredUsers = manager.getLootSplitPlayersWithUploadId(uploadID, splitID, guildID);
            System.out.printf("Number of registered users: %d\n", registeredUsers.size());
            int usersPerPage = 10;
            int totalPages = (int) (Math.ceil((double) registeredUsers.size() / usersPerPage));
    //        System.out.println("Number of users: " + registeredUsers.size());
            int currentPage = 0;
    //        System.out.println("Number of users with last seen: " + usersWithLastSeen.size());
            int totalUsersNum = registeredUsers.size();
            int counter = 0;
            if (totalUsersNum == 0) {
                EmbedBuilder newPage = new EmbedBuilder();
                newPage.setTitle("LootSplit Guild Upload - Page " + (currentPage + 1));
                newPage.setDescription("No users have been added, \nbecause none of the users in the list are registered in the database.");
                newPage.setFooter("Page 1 of 1");
                newPage.setColor(0x6064f4); // Blue
                pages.add(newPage.build());
                return pages;
            }
            for (int i = 0; i < registeredUsers.size(); i++) {
                EmbedBuilder newPage = new EmbedBuilder();
                newPage.setTitle("LootSplit Guild Upload - Page " + (currentPage + 1));
                StringBuilder pageContent = new StringBuilder();
                String header = " **Player Name**\n";
                for (int j = i; j < i + usersPerPage && j < totalUsersNum; j++) {
                    counter++;
                    String line = "- **" + registeredUsers.get(j) + "**\n";
                    pageContent.append(line);
                }
                newPage.setDescription("Total number of users added: **" + totalUsersNum + "**\n\n" + header + pageContent.toString());
                newPage.setFooter("Page " + (currentPage + 1) + " of " + (totalPages));
                newPage.setColor(0x6064f4); // Blue
                pages.add(newPage.build());
                currentPage++;
                i += usersPerPage - 1;
            }
        }
        catch (Exception e) {
            System.out.println("Error getting paginator embed: " + e.getMessage());
        }
        return pages;
//        System.out.println("Total number of users: " + counter);
    }

    public void addUsersToDatabase() throws SQLException {
        // Divide the list of character names into batches of 40
        List<String> playersBatch = new ArrayList<>();
        DatabaseManager manager = new DatabaseManager();
        for (int i = 0; i < players.size(); i++) {
            playersBatch.add(players.get(i));
            if (playersBatch.size() == 40 || i == players.size() - 1) {
                // Upload the data to the database
                try {
                    manager.addPlayersToLootSplit(uploadID, splitID, playersBatch, guildID);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Uploaded batch of " + playersBatch.size() + " users to the database");
                playersBatch.clear();
            }
        }
        manager.postProcessPlayersTable();
    }
}
