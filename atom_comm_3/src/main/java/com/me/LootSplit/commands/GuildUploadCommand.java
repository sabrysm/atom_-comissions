package com.me.LootSplit.commands;

import com.me.LootSplit.utils.GuildUploadHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import static com.me.LootSplit.utils.Messages.*;

public class GuildUploadCommand implements ISlashSubCommand{
    private CompletableFuture<Void> cf;
    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        File textFile;
        if (event.getOption("text") == null && event.getOption("file") == null) {
            sendErrorProvideTextOptionMessage(event);
            return;
        } else if (event.getOption("text") != null && event.getOption("file") != null) {
            sendErrorOnlyOneOptionAllowedMessage(event);
            return;
        } else if (event.getOption("text") != null) {
            String text = event.getOption("text").getAsString();
            String guildName = event.getOption("guild_name").getAsString();
            try {
                final GuildUploadHelper guildUploadHelperForText = new GuildUploadHelper(text, guildName, event.getGuild().getIdLong(), false);
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
                        String guildName = event.getOption("guild_name").getAsString();
                        final GuildUploadHelper guildUploadHelperForFile = new GuildUploadHelper(textFromFile, guildName, event.getGuild().getIdLong(), true);
                        guildUploadHelperForFile.uploadGuildUsers();
                        guildUploadHelperForFile.sendGuildUploadPaginator(event);
                        finalTextFile.delete();

                    } catch (IOException e) {
                        sendErrorMessage("IOException", "An error occurred while reading the GuildUpload file", 0xFF0000, event);
                        return;
                    } catch (SQLException e) {
                        sendErrorUploadMessage(event);
                        return;
                    } catch (Exception e) {
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
