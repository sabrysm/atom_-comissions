package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import com.me.LootSplit.utils.LootSplitSession;
import com.me.LootSplit.utils.LootSplitPartyUploadHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import static com.me.LootSplit.utils.Messages.sendNoActiveLootSplitSessionMessage;
import static com.me.LootSplit.utils.Messages.sendNoValidNamesFoundMessage;

public class LootSplitPartyUploadCommand implements ISlashSubCommand {

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        Message.Attachment image = event.getOption("party_image").getAsAttachment();
        InputStream imageStream = image.getProxy().download().join();
        LootSplitPartyUploadHelper lootSplitPartyUploadHelper;
        String splitID;
        try {
            splitID = LootSplitSession.getLootSplitIdFromGuild(event.getGuild().getIdLong());
            lootSplitPartyUploadHelper = new LootSplitPartyUploadHelper(splitID, event.getGuild().getIdLong());
            if (splitID == null) {
                sendNoActiveLootSplitSessionMessage(event);
                return;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Get the valid names from the image
        try {
            DatabaseManager manager = new DatabaseManager();
            List<String> validNames = lootSplitPartyUploadHelper.getValidNamesFromImage(imageStream);
            if (validNames.isEmpty()) {
                System.out.print("No valid names found\n");
                sendNoValidNamesFoundMessage(event);
                return;
            }

            // Add the players to the party
            lootSplitPartyUploadHelper.addPlayersToParty(event);
            System.out.printf("Valid Names: %s\n", validNames);
            // Send a success message
            lootSplitPartyUploadHelper.sendPartyUploadSuccessMessage(event);

        } catch (SQLException e) {
            System.out.printf("An SQL Exception occurred while adding the players to the party: %s\n", e.getMessage());
            sendErrorAddingPlayersToParty(event);
            return;
        } catch (NullPointerException e) {
            lootSplitPartyUploadHelper.sendCorrectCroppingFormat(event);
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
