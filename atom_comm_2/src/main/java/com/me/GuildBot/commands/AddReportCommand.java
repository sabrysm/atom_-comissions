package com.me.GuildBot.commands;

import com.me.GuildBot.database.DatabaseManager;
import com.me.GuildBot.utils.CTA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.SQLException;

public class AddReportCommand implements ISlashSubCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String report = event.getOption("report").getAsString();
        try {
            DatabaseManager manager = new DatabaseManager();
            String ctaID;
            if (event.getOption("cta_id") != null) {
                ctaID = event.getOption("cta_id").getAsString();
            } else {
                ctaID = CTA.getCTAIDFromGuild(event.getGuild().getIdLong());
            }
            ;
            if (!manager.CTAExists(ctaID, event.getGuild().getIdLong())) {
                CTA.sendCTANotFoundMessage(event, ctaID);
                return;
            }
            manager.addReport(ctaID, event.getGuild().getIdLong(), report);
            sendReportAddedMessage(event, ctaID);
        } catch (SQLException e) {
            System.out.printf("SQL Error occurred while adding report: %s\n", e.getMessage());
            sendErrorAddingReportMessage(event);
            return;
        }
    }

    public void sendReportAddedMessage(SlashCommandInteractionEvent event, String ctaID) {
        EmbedBuilder successEmbed = new EmbedBuilder();
        successEmbed.setTitle("Report Added").setDescription("The report has been added for the CTA with the ID **" + ctaID + "**").setColor(0x00FF00);
        event.getHook().sendMessageEmbeds(successEmbed.build()).queue();
    }

    /* Error Messages */

    // Error Adding Report
    private void sendErrorAddingReportMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("Error Adding Report").setDescription("An error occurred while adding the report").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }
}
