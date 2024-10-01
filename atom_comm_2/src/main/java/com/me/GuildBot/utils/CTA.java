package com.me.GuildBot.utils;

import com.me.GuildBot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;

import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CTA {
    // Attributes
    private final String id;
    private final long guildId;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
    private final String startTime;
    private final String endTime;


    // Create a random CTA ID of size 7
    public CTA(long guildId, String startTime, String endTime) {
        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        id = randomAlphaNumeric(7, ALPHA_NUMERIC_STRING);
        this.guildId = guildId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static Instant getInstantFromString(String time) {
        LocalDateTime localDateTime = LocalDateTime.parse(time, formatter);
        return localDateTime.atZone(ZoneId.of("UTC")).toInstant();
    }

    public static Instant getInstantFromStringTimeFromDB(String time) {
        LocalDateTime localDateTime = LocalDateTime.parse(time, formatter);
        return localDateTime.atZone(ZoneId.of("UTC")).toInstant();
    }

    public static String getStringFromInstant(Instant instant) {
        LocalDateTime localDateTime = instant.atZone(ZoneId.of("UTC")).toLocalDateTime();
        return localDateTime.format(formatter);
    }

    public boolean validateTimeFormat(String startTime, String endTime, SlashCommandInteractionEvent event) {
        try {
            Instant startInstant = getInstantFromString(startTime);
            Instant endInstant = getInstantFromString(endTime);
            if (startInstant.isAfter(endInstant)) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }



    public String getId() {
        return id;
    }

    // sendCTACancelledMessage(event, ctaID);
    public static void sendCTACancelledMessage(SlashCommandInteractionEvent event, String ctaID) {
        EmbedBuilder successEmbed = new EmbedBuilder();
        successEmbed.setTitle("CTA Cancelled").setDescription(String.format("The CTA with ID:**%s** has been cancelled", ctaID)).setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(successEmbed.build()).queue();
    }

    // sendCTADeletedMessage(event, ctaID);
    public static void sendCTADeletedMessage(SlashCommandInteractionEvent event, String ctaID) {
        EmbedBuilder successEmbed = new EmbedBuilder();
        successEmbed.setTitle("CTA Deleted").setDescription(String.format("The CTA with ID:**%s** has been deleted", ctaID)).setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(successEmbed.build()).queue();
    }

    // Get all the CTAs with info about (ctaID, startTime, endTime, status) in ctas
    public static List<MessageEmbed> getCTAsListPaginator(List<List<String>> ctas) {
        try {
            List<MessageEmbed> pages = new ArrayList<>();
            int namesPerPage = 5;
            int totalPages = (int) (Math.ceil((double) ctas.size() / namesPerPage));
            int currentPage = 0;
            if (ctas.size() == 0) {
                EmbedBuilder newPage = new EmbedBuilder();
                newPage.setTitle("CTA List");
                newPage.setDescription("No CTAs found");
                newPage.setColor(0x6064f4); // Blue
                pages.add(newPage.build());
                return pages;
            }
            for (int i = 0; i < ctas.size(); i++) {
                EmbedBuilder newPage = new EmbedBuilder();
                newPage.setTitle("CTA List");
                for (int j=i; j < i + namesPerPage && j < ctas.size(); j++) {
                    Instant startTime = getInstantFromString(ctas.get(j).get(1));
                    Instant endTime = getInstantFromString(ctas.get(j).get(2));
                    newPage.addField(ctas.get(j).get(0), String.format("- **Status:** `%s`\n- **Start Time:** %s\n- **End Time:** %s", StringUtils.capitalize(ctas.get(j).get(3)), "<t:" + startTime.getEpochSecond() + ":F>", "<t:" + endTime.getEpochSecond() + ":F>"), false);
                }
                newPage.setFooter("Page " + (currentPage + 1) + " of " + (totalPages));
                newPage.setColor(0x6064f4); // Blue
                pages.add(newPage.build());
                currentPage++;
                i += namesPerPage - 1;
            }
            return pages;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static String convertToFullTimestamp(String inputDateTime) throws ParseException {
        // Formatter for the input date-time string
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm");

            // Append the current year to the input string
            String dateTimeWithYear = inputDateTime + " " + Year.now().getValue();

            // Formatter for the new date-time string with the full pattern
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

            // Parse the input string with the year appended
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeWithYear, DateTimeFormatter.ofPattern("MM/dd HH:mm yyyy"));

            // Format the LocalDateTime to the full pattern
            return localDateTime.format(outputFormatter);
        } catch (Exception e) {
            throw new ParseException("Error parsing the date-time string", 0);
        }
    }

    public static String getCTAIDFromGuild(long guildId) throws SQLException {
        DatabaseManager databaseManager = new DatabaseManager();
        return databaseManager.getCTAID(guildId);
    }

    public void sendErrorMessage(SlashCommandInteractionEvent event, String description) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("Error Occurred").setDescription(description).setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
        return;
    }

    public void sendCTACreatedMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder successEmbed = new EmbedBuilder();
        Instant startInstant = getInstantFromString(startTime);
        Instant endInstant = getInstantFromString(endTime);
        successEmbed.setTitle("CTA Started").setDescription(String.format("A new CTA has been started with ID:**%s**", id)).setColor(0x00FF00).addField("Start Time", "<t:" + startInstant.getEpochSecond() + ":F>", true).addField("End Time", "<t:" + endInstant.getEpochSecond() + ":F>", true);
        event.getHook().sendMessageEmbeds(successEmbed.build()).queue();
    }

    private static String randomAlphaNumeric(int count, String ALPHA_NUMERIC_STRING) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    public static void sendUserStatsMessage(SlashCommandInteractionEvent event, String userName, int attendances, int lates, int absentees) {
        EmbedBuilder statsEmbed = new EmbedBuilder();
        statsEmbed.setTitle("Stats for " + userName);
        statsEmbed.addField("Attendances", String.valueOf(attendances), false);
        statsEmbed.addField("Lates", String.valueOf(lates), false);
        statsEmbed.addField("Absentees", String.valueOf(absentees), false);
        statsEmbed.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(statsEmbed.build()).queue();
    }

    public static void sendCTAInfoMessage(SlashCommandInteractionEvent event, String ctaID, Instant ctaStartTime, Instant ctaEndTime, int attendees, int lates, int absentees, String report) {
        EmbedBuilder infoEmbed = new EmbedBuilder();
        infoEmbed.setTitle("CTA Info");
        infoEmbed.setDescription("CTA ID: **" + ctaID + "**");
        infoEmbed.setColor(0x6064f4); // Blue color for the embed
        infoEmbed.addField("Start Time", "<t:" + ctaStartTime.getEpochSecond() + ":F>", false);
        infoEmbed.addField("End Time", "<t:" + ctaEndTime.getEpochSecond() + ":F>", false);
        infoEmbed.addField("Attendees", String.format("`%d`", attendees), true);
        infoEmbed.addField("Lates", String.format("`%d`", lates), true);
        infoEmbed.addField("Absentees", String.format("`%d`", absentees), true);
        if (report != null)
            infoEmbed.addField("Report", String.format("```%s```", report), false);
        event.getHook().sendMessageEmbeds(infoEmbed.build()).queue();
    }

    // sendNoCTAActiveMessage(event);
    public static void sendNoCTAActiveMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("No Active CTA").setDescription("There is no active CTA for this Guild at the moment").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
        return;
    }

    public static void sendCTAAlreadyActiveMessage(SlashCommandInteractionEvent event, String ctaID) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("CTA Already Active").setDescription(String.format("A CTA is already active for this server\n\nCTA ID:**%s**", ctaID)).setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
        return;
    }

    public static void sendCTANotFoundMessage(SlashCommandInteractionEvent event, String ctaID) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("CTA Not Found").setDescription("The CTA with the ID **" + ctaID + "** was not found").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
        return;
    }

}
