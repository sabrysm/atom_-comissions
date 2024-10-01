package com.me.GuildBot.commands;

import com.me.GuildBot.database.DatabaseManager;
import com.me.GuildBot.utils.CTA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.SQLException;

public class LateRegisterCommand implements ISlashSubCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String username = event.getOption("username").getAsString();
        try {
            String ctaID = CTA.getCTAIDFromGuild(event.getGuild().getIdLong());
            if (ctaID == null) {
                sendNoCTAActiveMessage(event);
                return;
            }
            DatabaseManager manager = new DatabaseManager();
            manager.addPlayerToParty(username, event.getGuild().getIdLong(), ctaID, null, "late");
            EmbedBuilder successEmbed = new EmbedBuilder();
            successEmbed.setTitle("User Added").setDescription("The user **" + username + "** has been added to the CTA as a **Late Attendee**").setColor(0x6064f4);
            event.getHook().sendMessageEmbeds(successEmbed.build()).queue();
        } catch (SQLException e) {
            System.out.printf("SQL Error occurred while adding user as a Late Register: %s\n", e.getMessage());
            sendErrorAddingUserMessage(event);
            return;
        }
    }

    /* Error Messages */

    // No Active CTA
    private void sendNoCTAActiveMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("No CTA").setDescription("No CTA is currently active for this server").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    // Error Adding User
    private void sendErrorAddingUserMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("Error Adding User").setDescription("An error occurred while adding the user").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }
}
