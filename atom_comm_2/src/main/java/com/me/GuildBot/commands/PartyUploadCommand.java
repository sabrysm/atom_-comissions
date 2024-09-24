package com.me.GuildBot.commands;

import com.me.GuildBot.database.DatabaseManager;
import com.me.GuildBot.utils.CTA;
import com.me.GuildBot.utils.PartyUploadHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

public class PartyUploadCommand implements ISlashSubCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        int partyNumber = event.getOption("party_number").getAsInt();
        Message.Attachment image = event.getOption("party_image").getAsAttachment();
        InputStream imageStream = image.getProxy().download().join();
        PartyUploadHelper partyUploadHelper;
        String ctaID;
        try {
            ctaID = CTA.getCTAIDFromGuild(event.getGuild().getIdLong());
            partyUploadHelper = new PartyUploadHelper(ctaID, event.getGuild().getIdLong(), partyNumber);
            if (ctaID == null) {
                partyUploadHelper.sendNoCTAActiveMessage(event);
                return;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Get the valid names from the image
        try {
            DatabaseManager manager = new DatabaseManager();
            List<String> validNames = partyUploadHelper.getValidNamesFromImage(imageStream);
            // Clear the users data for the party before adding the new data
            manager.clearUsersDataForParty(event.getGuild().getIdLong(), ctaID, partyNumber);
            System.out.printf("Cleared users history for party %d", partyNumber);
            if (validNames.isEmpty()) {
                System.out.print("No valid names found\n");
                partyUploadHelper.sendNoValidNamesFoundMessage(event);
                return;
            }

            // Add the players to the party
            partyUploadHelper.addPlayersToParty(event);
            System.out.printf("Valid Names: %s\n", validNames);
            // Send a success message
            partyUploadHelper.sendPartyUploadSuccessMessage(event);

        } catch (SQLException e) {
            System.out.printf("An SQL Exception occurred while adding the players to the party: %s\n", e.getMessage());
            sendErrorAddingPlayersToParty(event);
            return;
        } catch (NullPointerException e) {
            partyUploadHelper.sendCorrectCroppingFormat(event);
            return;
        } catch (Exception e) {
            System.out.printf("A General Exception occurred while uploading the party: %s\n", e.getMessage());
            sendErrorProcessingImage(event);
            return;
        }
    }

    /* Error messages as Embeds */

    // Error Processing the image
    public void sendErrorProcessingImage(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Error Processing Image").setDescription("An error occurred while processing the image").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
    }

    // Error adding players to the party
    public void sendErrorAddingPlayersToParty(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Error Adding Players").setDescription("An error occurred while adding the players to the party").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
    }
}
