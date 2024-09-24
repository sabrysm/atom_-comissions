package com.me.GuildBot.commands;

import com.me.GuildBot.database.DatabaseManager;
import com.me.GuildBot.utils.CTA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.SQLException;
import java.time.Instant;

public class InfoCommand implements ISlashSubCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String ctaID = event.getOption("cta_id").getAsString();
        try {
            DatabaseManager manager = new DatabaseManager();
            if (!manager.CTAExists(ctaID, event.getGuild().getIdLong())) {
                CTA.sendCTANotFoundMessage(event, ctaID);
                return;
            }
            // Update the attendees and lates
            manager.updateCTAStats(ctaID, event.getGuild().getIdLong());
            // Get the CTA info
            Instant ctaStartTime = manager.getCTAStartTime(ctaID, event.getGuild().getIdLong());
            Instant ctaEndTime = manager.getCTAEndTime(ctaID, event.getGuild().getIdLong());
            String ctaReport = manager.getReportForCTA(ctaID, event.getGuild().getIdLong());
            // Get All attendees
            int attendees = manager.getAttendeesForCTA(ctaID, event.getGuild().getIdLong());
            // Get All lates
            int lates = manager.getLatesForCTA(ctaID, event.getGuild().getIdLong());
            // Get All absentees
            int absentees = manager.getAbsenteesForCTA(ctaID, event.getGuild().getIdLong());
            // Send the info message
            CTA.sendCTAInfoMessage(event, ctaID, ctaStartTime, ctaEndTime, attendees, lates, absentees, ctaReport);
        } catch (SQLException e) {
            System.out.printf("SQL Error occurred while getting CTA info: %s\n", e.getMessage());
            sendErrorGettingCTAInfoMessage(event);
            return;
        }
    }

    /* Error Messages */

    // Error Getting CTA Info
    private void sendErrorGettingCTAInfoMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("Error Getting CTA Info").setDescription("An error occurred while getting the CTA info").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }
}
