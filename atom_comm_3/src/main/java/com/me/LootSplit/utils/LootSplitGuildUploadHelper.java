package com.me.LootSplit.utils;

import com.me.LootSplit.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

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
    private final boolean isFile;

    public LootSplitGuildUploadHelper(String text, String splitID, long guildID, boolean isFile) throws Exception {
        this.guildID = guildID;
        this.splitID = splitID;
        this.isFile = isFile;
        parseText(text);
    }


    public void uploadGuildUsers() throws SQLException {
        // Add the users to the database
        addUsersToDatabase();
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
        int usersPerPage = 10;
        int totalPages = (int) (Math.ceil((double) players.size() / usersPerPage));
//        System.out.println("Number of users: " + players.size());
        int currentPage = 0;
//        System.out.println("Number of users with last seen: " + usersWithLastSeen.size());
        int totalUsersNum = players.size();
        int counter = 0;
        for (int i = 0; i < players.size(); i++) {
            EmbedBuilder newPage = new EmbedBuilder();
            newPage.setTitle("LootSplit Guild Upload - Page " + (currentPage + 1));
            StringBuilder pageContent = new StringBuilder();
            String header = " **Player Name**\n";
            for (int j = i; j < i + usersPerPage && j < totalUsersNum; j++) {
                counter++;
                String line = "- **" + players.get(j) + "\n";
                pageContent.append(line);
            }
            newPage.setDescription("Total number of users added: **" + totalUsersNum + "**\n\n" + header + pageContent.toString());
            newPage.setFooter("Page " + (currentPage + 1) + " of " + (totalPages));
            newPage.setColor(0x6064f4); // Blue
            pages.add(newPage.build());
            currentPage++;
            i += usersPerPage - 1;
        }
//        System.out.println("Total number of users: " + counter);
        return pages;
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
                    manager.addPlayersToLootSplit(splitID, playersBatch, guildID);
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
