package com.me.GuildBot.commands;

import com.me.GuildBot.database.DatabaseManager;
import com.me.GuildBot.utils.CTA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.SQLException;
import java.text.ParseException;
import java.time.Instant;

public class StartCommand implements ISlashSubCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Get the start and end time
        String startTime = event.getOption("start_time").getAsString();
        String endTime = event.getOption("end_time").getAsString();
        // If a CTA is already active, send an error message
        try {
            String ctaID = CTA.getCTAIDFromGuild(event.getGuild().getIdLong());
            startTime = CTA.convertToFullTimestamp(startTime);
            endTime = CTA.convertToFullTimestamp(endTime);
            if (ctaID != null) {
                CTA.sendCTAAlreadyActiveMessage(event, ctaID);
                return;
            }
        } catch (SQLException e) {
            System.out.printf("Error checking for active CTA: %s\n", e.getMessage());
            sendErrorCheckingActiveCTAMessage(event);
            return;
        } catch (ParseException e) {
            System.out.printf("Error parsing time: %s\n", e.getMessage());
            sendErrorParsingTimeMessage(event);
            return;
        }
        // Create a new CTA
        CTA cta = new CTA(event.getGuild().getIdLong(), startTime, endTime);
        // Validate the time
        if (!cta.validateTimeFormat(startTime, endTime, event)) {
            cta.sendErrorMessage(event, "You need to provide a valid time in the format `MM/dd HH:mm`");
            return;
        }
        if (CTA.getInstantFromString(endTime).isBefore(Instant.now())) {
            cta.sendErrorMessage(event, "The end time cannot be in the past");
            return;
        }
        try {
            // Make datetime format from the string
            DatabaseManager manager = new DatabaseManager();
            manager.createCTA(cta.getId(), event.getGuild().getIdLong(), startTime, endTime);
            // Send a success message in an embed with the CTA ID
            cta.sendCTACreatedMessage(event);

        } catch (SQLException e) {
            System.out.printf("Error creating CTA: %s\n", e.getMessage());
            sendErrorConnectingDatabaseMessage(event);
            return;
        }
    }

    /* Error messages as Embeds */
    private void sendErrorConnectingDatabaseMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Connection Error").setDescription("An error occurred while connecting to the database").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void sendErrorStartingCTAMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Error Starting CTA").setDescription("An error occurred while starting the CTA").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void sendErrorParsingTimeMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Error Parsing Time").setDescription("An error occurred while parsing the time").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
    }

    public void sendErrorCheckingActiveCTAMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Error Checking").setDescription("An error occurred while checking for active CTAs").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
    }
}
