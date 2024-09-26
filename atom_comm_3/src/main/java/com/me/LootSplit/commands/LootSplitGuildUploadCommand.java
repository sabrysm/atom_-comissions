package com.me.LootSplit.commands;

import com.me.LootSplit.utils.GuildUploadHelper;
import com.me.LootSplit.utils.LootSplitGuildUploadHelper;
import com.me.LootSplit.utils.LootSplitSession;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import static com.me.LootSplit.utils.Messages.sendErrorProvideTextOptionMessage;
import static com.me.LootSplit.utils.Messages.sendErrorOnlyOneOptionAllowedMessage;
import static com.me.LootSplit.utils.Messages.sendErrorConnectingDatabaseMessage;
import static com.me.LootSplit.utils.Messages.sendErrorUploadMessage;
import static com.me.LootSplit.utils.Messages.sendErrorReadingMessage;
import static com.me.LootSplit.utils.Messages.sendNotGuildListMessage;

public class LootSplitGuildUploadCommand implements ISlashSubCommand{
    private CompletableFuture<Void> cf;
    private String lootSplitId;

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        File textFile;

        try {
            lootSplitId = LootSplitSession.getLootSplitIdFromGuild(event.getGuild().getIdLong());
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorConnectingDatabaseMessage(event);
            return;
        }
        if (event.getOption("text") == null && event.getOption("file") == null) {
            sendErrorProvideTextOptionMessage(event);
            return;
        } else if (event.getOption("text") != null && event.getOption("file") != null) {
            sendErrorOnlyOneOptionAllowedMessage(event);
            return;
        } else if (event.getOption("text") != null) {
            String text = event.getOption("text").getAsString();
            try {
                final LootSplitGuildUploadHelper guildUploadHelperForText = new LootSplitGuildUploadHelper(text, lootSplitId, event.getGuild().getIdLong(), false);
                guildUploadHelperForText.uploadGuildUsers();
                guildUploadHelperForText.sendGuildUploadPaginator(event);
            } catch (SQLException e) {
                e.printStackTrace();
                sendErrorUploadMessage(event);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                sendErrorReadingMessage(event);
                return;
            }
        } else if (event.getOption("file") != null && !event.getOption("file").getAsAttachment().isImage() && !event.getOption("file").getAsAttachment().isVideo()) {
            // Get the text from the file
            textFile = new File(event.getOption("file").getAsAttachment().getFileName().getBytes(StandardCharsets.UTF_8).toString());
            cf = event.getOption("file").getAsAttachment().getProxy().downloadToFile(textFile).thenAccept(file -> {
                // Send a confirmation message to Discord
                System.out.println("File downloaded and read successfully!");
            });

            File finalTextFile = textFile;
            cf.whenComplete((result, error) -> {
                if (error != null) {
                    System.out.println("Exception: " + error.getMessage());
                } else {
                    try {
                        final String textFromFile = new String(Files.readAllBytes(finalTextFile.toPath()));
                        final LootSplitGuildUploadHelper guildUploadHelperForFile = new LootSplitGuildUploadHelper(textFromFile, lootSplitId, event.getGuild().getIdLong(), true);
                        guildUploadHelperForFile.uploadGuildUsers();
                        guildUploadHelperForFile.sendGuildUploadPaginator(event);
                        finalTextFile.delete();

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        sendErrorUploadMessage(event);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendErrorReadingMessage(event);
                        return;
                    }
                }
            });
        } else {
            sendNotGuildListMessage(event);
            return;
        }
    }
}