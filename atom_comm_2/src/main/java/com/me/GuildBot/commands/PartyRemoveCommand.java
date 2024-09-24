package com.me.GuildBot.commands;

import com.me.GuildBot.utils.CTA;
import com.me.GuildBot.utils.PartyUploadHelper;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.SQLException;

public class PartyRemoveCommand implements ISlashSubCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Get the party number and the username
        int partyNumber = event.getOption("party_number").getAsInt();
        String playerName = event.getOption("username").getAsString();
        PartyUploadHelper partyUploadHelper;
        try {
            String ctaID = CTA.getCTAIDFromGuild(event.getGuild().getIdLong());
            partyUploadHelper = new PartyUploadHelper(ctaID, event.getGuild().getIdLong(), partyNumber);
            if (ctaID == null) {
                partyUploadHelper.sendNoCTAActiveMessage(event);
                return;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        // Remove the player from the party
        partyUploadHelper.removeUserFromParty(playerName, event);
        // Send a success message
        partyUploadHelper.sendUserRemovedFromPartyEmbed(playerName, event);
    }
}
