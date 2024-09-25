package com.me.LootSplit.commands;

import com.me.LootSplit.utils.CTA;
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

public class GuildUploadCommand implements ISlashSubCommand{
    private CompletableFuture<Void> cf;

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        File textFile;
        String ctaID;

        try {
            ctaID = CTA.getCTAIDFromGuild(event.getGuild().getIdLong());
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
                final GuildUploadHelper guildUploadHelperForText = new GuildUploadHelper(text, ctaID, event.getGuild().getIdLong(), false);
                guildUploadHelperForText.uploadGuildUploadUsers(ctaID, event);
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
                        final GuildUploadHelper guildUploadHelperForFile = new GuildUploadHelper(textFromFile, ctaID, event.getGuild().getIdLong(), true);
                        guildUploadHelperForFile.uploadGuildUploadUsers(ctaID, event);
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

    /* Error Messages */

    public void sendNotGuildListMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("Invalid Format").setDescription("The provided is not a Guild List").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public void sendErrorUploadMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("An error occurred while uploading the data").setDescription("An error occurred while uploading the data").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public void sendErrorReadingMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("An error occurred while reading the data").setDescription("An error occurred while reading the data").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public void sendErrorConnectingDatabaseMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("An error occurred while connecting to the database").setDescription("An error occurred while connecting to the database").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public void sendErrorOnlyOneOptionAllowedMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("Only one option is allowed").setDescription("Only one option is allowed").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public void sendErrorProvideTextOptionMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("You need to provide a text option").setDescription("You need to provide a text option").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }
}
