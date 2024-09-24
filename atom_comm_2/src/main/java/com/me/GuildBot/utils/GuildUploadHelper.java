package com.me.GuildBot.utils;

import com.me.GuildBot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuildUploadHelper {
    /**
     * Converts a CSV string to a 2D array
     *
     * @param text The CSV string
     * @return The 2D array
     */

    private final List<List<String>> roles = new ArrayList<>();
    private final List<String> charNames = new ArrayList<>();
    private final List<String> lastSeen = new ArrayList<>();
    private final List<String> onlineUsers = new ArrayList<>();
    private long guildID;
    private String ctaID;
    private boolean isFile;

    public GuildUploadHelper(String text, String ctaID, long guildID, boolean isFile) throws Exception {
        this.ctaID = ctaID;
        this.guildID = guildID;
        this.isFile = isFile;
        parseText(text);
    }

    public void uploadGuildUploadUsers(String ctaID, SlashCommandInteractionEvent event) throws Exception, SQLException {
        DatabaseManager manager = new DatabaseManager();
        if (ctaID == null) {
            EmbedBuilder errorEmbed = new EmbedBuilder();
            errorEmbed.setTitle("No CTA").setDescription("No CTA is currently active for this server").setColor(0xFF0000);
            event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
            return;
        }
        // Clear the users data for the guild before adding the new data
        manager.deleteUsersDataForGuild(event.getGuild().getIdLong(), ctaID);
        // Add the users to the database
        addUsersToDatabase();
    }

    public void updateGuildUploadUsers(String ctaID, SlashCommandInteractionEvent event) throws Exception, SQLException {
        try {
            DatabaseManager manager = new DatabaseManager();
            if (ctaID == null) {
                EmbedBuilder errorEmbed = new EmbedBuilder();
                errorEmbed.setTitle("No CTA").setDescription("No CTA is currently active for this server").setColor(0xFF0000);
                event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
                return;
            }
            // Add the users to the database
            updateUsersLastSeen();

        } catch (Exception e) {
            e.printStackTrace();
            EmbedBuilder errorEmbed = new EmbedBuilder();
            errorEmbed.setTitle("An error occurred while uploading the data").setDescription("An error occurred while uploading the data").setColor(0xFF0000);
            event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
            return;
        }
    }

    public void updateUsersLastSeen() throws Exception, SQLException {
        // Divide the list of character names and last seen dates into batches of 30
        List<String> charNamesBatch = new ArrayList<>();
        List<String> lastSeenBatch = new ArrayList<>();
        DatabaseManager manager = new DatabaseManager();
        for (int i = 0; i < charNames.size(); i++) {
//            System.out.println("Index: " + i + "CharName size: " + charNames.size() + " LastSeen size: " + lastSeen.size());
//            System.out.println("CharName: " + charNames.get(i) + " LastSeen: " + lastSeen.get(i));
            charNamesBatch.add(charNames.get(i));
            lastSeenBatch.add(lastSeen.get(i));
            if (charNamesBatch.size() == 40 || i == charNames.size() - 1) {
                // Upload the data to the database
                try {
                    manager.updateLastSeenGuildData(guildID, ctaID, charNamesBatch, lastSeenBatch);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Updated batch of " + charNamesBatch.size() + " users to the database ending with " + charNamesBatch.get(charNamesBatch.size() - 1));
                charNamesBatch.clear();
                lastSeenBatch.clear();
            }
        }

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
//                System.out.println("Lines length: " + lines.length);
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

            // Process each line
            for (int i = 1; i < result.length; i++) {
                for (int j = 0; j < result[1].length; j++) {
                    switch (j) {
                        case 0:
                            charNames.add(result[i][j]);
                            break;
                        case 1:
                            // System.out.println("Case 1: \"" + result[i][j] + "\"");
                            if (result[i][j].toLowerCase(Locale.ROOT).equals("online")) {
                                onlineUsers.add(result[i][0]);
                            // System.out.println("Online user: " + result[i][0] + " Last Seen: " + CTA.getStringFromInstant(Instant.now()));
                                lastSeen.add(CTA.getStringFromInstant(Instant.now()));
                                break;
                            }
                            else {
                                lastSeen.add(result[i][j].trim());
                            }
                            break;
                        case 2:
//                            roles.add(Arrays.asList(result[i][j].split(";")));
                            break;
                    }
                }
            }
//            System.out.println("Size of lastSeen: " + lastSeen.size());
        } catch (Exception e) {
            System.out.println("Error parsing the text: " + e.getMessage());
        }

    }

    public List<List<String>> getUsersWithLastSeenOrdered() {
        List<List<String>> result = new ArrayList<>();
        try {
            DatabaseManager manager = new DatabaseManager();
            List<String> dbResult = manager.getUsersWithLastSeenForGuildOrderedDesc(guildID, ctaID);
            for (String line : dbResult) {
                List<String> temp = new ArrayList<>();
                String[] lineSplit = line.split("\\|\\|\\|");
                temp.add(lineSplit[0]);
                temp.add(lineSplit[1]);
//                System.out.println("UserWithLastSeenOrdered: " + lineSplit[0] + " Last Seen: " + lineSplit[1]);
                result.add(temp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }


    public List<MessageEmbed> getPaginatorEmbed() {
        List<MessageEmbed> pages = new ArrayList<>();
        int usersPerPage = 10;
        int totalPages = (int) (Math.ceil((double) charNames.size() / usersPerPage));
//        System.out.println("Number of users: " + charNames.size());
        int currentPage = 0;
        List<List<String>> usersWithLastSeen = getUsersWithLastSeenOrdered();
//        System.out.println("Number of users with last seen: " + usersWithLastSeen.size());
        int totalUsersNum = usersWithLastSeen.size();
        int counter = 0;
        for (int i = 0; i < usersWithLastSeen.size(); i++) {
            EmbedBuilder newPage = new EmbedBuilder();
            newPage.setTitle("Guild List - Page " + (currentPage + 1));
            StringBuilder pageContent = new StringBuilder();
            String header = " **Character Name**  -  **Last Seen**\n";
            for (int j = i; j < i + usersPerPage && j < totalUsersNum; j++) {
                counter++;
                String line = "- **" + usersWithLastSeen.get(j).get(0) + "** - `" + usersWithLastSeen.get(j).get(1) + "`\n";
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

    public void addUsersToDatabase() throws Exception, SQLException {
        // Divide the list of character names and last seen dates into batches of 30
        List<String> charNamesBatch = new ArrayList<>();
        List<String> lastSeenBatch = new ArrayList<>();
        DatabaseManager manager = new DatabaseManager();
//        System.out.println("charNames size: " + charNames.size() + " lastSeen size: " + lastSeen.size());
        for (int i = 0; i < charNames.size(); i++) {
            charNamesBatch.add(charNames.get(i));
            System.out.println(lastSeen.get(i));
            lastSeenBatch.add(lastSeen.get(i));
            if (charNamesBatch.size() == 40 || i == charNames.size() - 1) {
                // Upload the data to the database
                try {
//                    System.out.println("Uploading batch of " + charNamesBatch.size() + " users");
                    manager.uploadGuildData(guildID, ctaID, charNamesBatch, lastSeenBatch, "offline");
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Uploaded batch of " + charNamesBatch.size() + " users to the database ending with " + charNamesBatch.get(charNamesBatch.size() - 1));
                charNamesBatch.clear();
                lastSeenBatch.clear();
            }
        }
        for (String onlineUser : onlineUsers) {
            manager.updateUserStatus(guildID, ctaID, onlineUser, "absent");
        }
        manager.postProcessUsersTable();
    }
}
