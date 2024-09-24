package com.me.GuildBot.utils;

import com.me.GuildBot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GuildCheckHelper {
    private final String ctaID;
    private final long guildID;

    public GuildCheckHelper(String ctaID, long guildID) {
        this.ctaID = ctaID;
        this.guildID = guildID;
    }

    public void sendGuildCheckPaginator(SlashCommandInteractionEvent event) {
        List<MessageEmbed> guildEmbeds = getPaginatorEmbed();
        Paginator guildPaginator = new Paginator(guildEmbeds, event.getUser().getIdLong());
        event.getJDA().addEventListener(guildPaginator);
        guildPaginator.sendInitialMessage(event);
    }

    public String getPlayerStatus(String playerName) {
        String status = "offline";
        try {
            DatabaseManager databaseManager = new DatabaseManager();
            String statusFromDB = databaseManager.getPlayerStatus(guildID, ctaID, playerName);
            if (statusFromDB == null) {
                status = "offline";
            }
            else if (statusFromDB.equals("absent") && databaseManager.playerHasParty(playerName, guildID, ctaID)) {
                status = "present";
            } else if (statusFromDB.equals("absent") && !databaseManager.playerHasParty(playerName, guildID, ctaID))  {
                status = "absent";
            }
            databaseManager.updateUserStatus(guildID, ctaID, playerName, status);
        } catch (SQLException e) {
            // Either last seen or start time not found
            System.out.println("Get Player Status Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return status;
    }

    public List<MessageEmbed> getPaginatorEmbed() {
        List<MessageEmbed> pages = new ArrayList<>();
        List<Integer> parties;
        try {
            System.out.println("Initiating Variables");
            DatabaseManager manager = new DatabaseManager();
            List<String> absentees = manager.getAbsentees(guildID, ctaID);
            parties = manager.getParties(ctaID, guildID);
            System.out.printf("Size of parties: %s\n", parties.size());
            int namesPerPage = 20;
            int absenteesPagesCount =(int) Math.ceil((double) absentees.size() / namesPerPage);
            int totalPages = parties.size() + absenteesPagesCount;
            int currentPage = 0;
            if (parties.isEmpty() && absentees.isEmpty()) {
                EmbedBuilder errorPage = new EmbedBuilder();
                errorPage.setTitle("Check Guild Members");
                errorPage.setDescription("No online members in the guild have been detected.");
                errorPage.setColor(0x6064f4); // Blue
                pages.add(errorPage.build());
                return pages;
            }
            System.out.println("Iterating through parties");
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                EmbedBuilder newPage = new EmbedBuilder();
                List<String> partyMembersNames = new ArrayList<>();
                List<String> partyMembersStatus = new ArrayList<>();
                Integer partyNumber = null;
                if (pageIndex >= absenteesPagesCount) {
                    partyNumber = parties.get(pageIndex - absenteesPagesCount);
                    List<String> partyMembers = manager.getPartyMembers(ctaID, guildID, partyNumber);
//                namesPerPage = partyMembers.isEmpty() ? 1 : partyMembers.size();
                    System.out.println("Party Members: " + partyMembers);
                    for (String line : partyMembers) {
                        String[] split = line.split("\\|\\|\\|");
                        String playerName = split[0];
                        String status = split[1];
                        System.out.println("Player: " + playerName + " Status: " + status);
                        partyMembersNames.add(playerName);
                        partyMembersStatus.add(status);
                    }
                }
                System.out.println("Checkpoint 1");
                newPage.setTitle("Check Guild Members");
                StringBuilder pageContent = new StringBuilder();
                String header;
                // Use StringUtils.center to center the text with 20 size for #, 20 for name and 20 for status (60 total)
                if (pageIndex < absenteesPagesCount) {
                    header = "```\n" + StringUtils.center("<< Absentees >>", 60) + "\n" + StringUtils.left("#", 25) + StringUtils.center("Name", 40) + StringUtils.right("Status", 25) + "\n\n";
                } else {
                    header = "```\n" + StringUtils.center(String.format("<< Party %s >>", partyNumber), 60) + "\n" + StringUtils.left("#", 25) + StringUtils.center("Name", 40) + StringUtils.right("Status", 25) + "\n\n";
                }
                pageContent.append(header);
                System.out.println("Checkpoint 2");
                if (pageIndex < absenteesPagesCount) {
                    for (int j = namesPerPage*pageIndex; j < namesPerPage*pageIndex + namesPerPage && j < absentees.size(); j++) {
                        String playerName = absentees.get(j);
                        String status = getPlayerStatus(playerName);
                        pageContent.append(StringUtils.left(String.valueOf(j + 1), 25)).append(StringUtils.center(playerName, 40)).append(StringUtils.right(status, 25)).append("\n");
                    }
                } else {
                    for (int j = 0; j < namesPerPage && j < partyMembersNames.size(); j++) {
                        // Add fields to the embed and make it as a table
                        pageContent.append(StringUtils.left(String.valueOf(j + 1), 25)).append(StringUtils.center(partyMembersNames.get(j), 40)).append(StringUtils.right(partyMembersStatus.get(j), 25)).append("\n");
                    }
                }
                System.out.println("Checkpoint 3");
                if (partyMembersNames.isEmpty() && pageIndex >= absenteesPagesCount)
                    pageContent.append(StringUtils.center("No members detected", 60)).append("\n");
                pageContent.append("```");
                System.out.println("Checkpoint 4");
                if (pageIndex < absenteesPagesCount) {
                    newPage.setDescription("The following usernames have been detected as absentees:\n\n" + pageContent.toString());

                } else {
                    newPage.setDescription(String.format("The following usernames have been detected for Party number **%s**:\n*(You can re-upload the image to update the party)*\n\n" + pageContent.toString(), partyNumber));
                }
                System.out.println("Checkpoint 5");
                newPage.setFooter("Page " + (currentPage + 1) + " of " + (totalPages));
                newPage.setColor(0x6064f4); // Blue
                pages.add(newPage.build());
                currentPage++;
                System.out.println("Checkpoint 6");
            }
            System.out.printf("Pages: %s", pages.size());
            return pages;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
