package com.me.GuildBot.commands;

import com.me.GuildBot.database.DatabaseManager;
import com.me.GuildBot.utils.CTA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.SQLException;
import java.time.Instant;

public class DeleteCommand implements ISlashSubCommand {
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        try {
            String ctaID = CTA.getCTAIDFromGuild(event.getGuild().getIdLong());
            if (ctaID == null) {
                CTA.sendNoCTAActiveMessage(event);
                return;
            }
            DatabaseManager manager1 = new DatabaseManager();
            manager1.updateCTAStatus(ctaID, "finished");
            manager1.updateCTAFinishTime(ctaID, event.getGuild().getIdLong(), Instant.now());
            CTA.sendCTADeletedMessage(event, ctaID);
        } catch (SQLException e) {
            System.out.printf("An error occurred while cancelling the CTA in Delete Command: %s\n", e.getMessage());
            sendErrorCancellingCTAMessage(event);
            return;
        }
    }

    /* Error Messages */

    public void sendErrorCancellingCTAMessage(SlashCommandInteractionEvent event) {
        event.getHook().sendMessage("An error occurred while cancelling the CTA").setEphemeral(true).queue();
    }
}
