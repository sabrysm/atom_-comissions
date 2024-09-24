package com.me.GuildBot.commands;

import com.me.GuildBot.database.DatabaseManager;
import com.me.GuildBot.utils.CTA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.SQLException;

public class StatsCommand implements ISlashSubCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userName = event.getOption("username").getAsString();
        try {
            DatabaseManager manager = new DatabaseManager();
            String ctaID = CTA.getCTAIDFromGuild(event.getGuild().getIdLong());
            if (ctaID == null) {
                CTA.sendNoCTAActiveMessage(event);
                return;
            }
            Integer attendances = manager.getAttendancesForUser(userName, event.getGuild().getIdLong());
            Integer lates = manager.getLatesForUser(userName, event.getGuild().getIdLong());
            Integer absentees = manager.getAbsenteesForUser(userName, event.getGuild().getIdLong());
            CTA.sendUserStatsMessage(event, userName, attendances, lates, absentees);
        } catch (SQLException e) {
            System.out.printf("SQL Error occurred while getting stats for user: %s\n", e.getMessage());
            sendErrorGettingStatsMessage(event);
            return;
        }
    }

    /* Error Messages */

    // Error Getting Stats
    private void sendErrorGettingStatsMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("Error Getting Stats").setDescription("An error occurred while getting the stats").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }
}
