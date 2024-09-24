package com.me.GuildBot.commands;

import com.me.GuildBot.database.DatabaseManager;
import com.me.GuildBot.utils.CTA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.SQLException;

public class CancelCommand implements ISlashSubCommand {
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        try {
            String ctaID = CTA.getCTAIDFromGuild(event.getGuild().getIdLong());
            if (ctaID == null) {
                CTA.sendNoCTAActiveMessage(event);
                return;
            }
            DatabaseManager manager = new DatabaseManager();
            manager.deleteCTA(ctaID, event.getGuild().getIdLong());
            CTA.sendCTACancelledMessage(event, ctaID);
        } catch (SQLException e) {
            System.out.printf("Error cancelling CTA: %s\n", e.getMessage());
            sendErrorCancelCTAMessage(event);
            return;
        }
    }

    /* Error Messages */

    public void sendErrorCancelCTAMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Error");
        embed.setDescription("An error occurred while cancelling the CTA");
        event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
    }
}
