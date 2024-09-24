package com.me.GuildBot.commands;

import com.me.GuildBot.utils.CTA;
import com.me.GuildBot.utils.GuildCheckHelper;
import com.me.GuildBot.utils.GuildUploadHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class GuildCheckCommand implements ISlashSubCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Get the text option
        File textFileForCheck;
        String cta_ID;
        // Get the CTA ID and
        try {
            cta_ID = CTA.getCTAIDFromGuild(event.getGuild().getIdLong());
            if (cta_ID == null) {
                CTA.sendNoCTAActiveMessage(event);
                return;
            }
        } catch (SQLException e) {
            System.out.printf("SQL Error occurred while connecting to the database: %s\n", e.getMessage());
            sendErrorConnectingDatabaseMessage(event);
            return;
        }
        try {
            if (event.getOption("text") == null && event.getOption("file") == null) {
                // If no option is provided, send Guild Check Paginator for the current Guild List
                GuildCheckHelper guildCheckHelper = new GuildCheckHelper(cta_ID, event.getGuild().getIdLong());
                guildCheckHelper.sendGuildCheckPaginator(event);
                return;
            } else if (event.getOption("text") != null && event.getOption("file") != null) {
                sendErrorProvidingOptionsMessage(event);
                return;
            } else if (event.getOption("text") != null) {
                // Get the text from the text option
                String text= event.getOption("text").getAsString();
                GuildUploadHelper guildUploadHelperForText = new GuildUploadHelper(text, cta_ID, event.getGuild().getIdLong(), false);
                guildUploadHelperForText.updateGuildUploadUsers(cta_ID, event);
                GuildCheckHelper guildCheckHelper = new GuildCheckHelper(cta_ID, event.getGuild().getIdLong());
                guildCheckHelper.sendGuildCheckPaginator(event);
            } else if (event.getOption("file") != null && !event.getOption("file").getAsAttachment().isImage() && !event.getOption("file").getAsAttachment().isVideo()) {
                // Get the text from the file
                textFileForCheck = new File(event.getOption("file").getAsAttachment().getFileName().getBytes(StandardCharsets.UTF_8).toString());
                // Send a confirmation message to Discord
                CompletableFuture<Void> cf = event.getOption("file").getAsAttachment().getProxy().downloadToFile(textFileForCheck).thenAccept(file -> {
                    // Send a confirmation message to Discord
                    System.out.println("File downloaded and read successfully!");
                });

                File finalTextFile = textFileForCheck;
                cf.whenComplete((result, error) -> {
                    if (error != null) {
                        System.out.println("Exception: " + error.getMessage());
                    } else {
                        String textFromFile = null;
                        try {
                            textFromFile = new String(Files.readAllBytes(finalTextFile.toPath()));
                        } catch (IOException e) {
                            System.out.printf("Error reading the file: %s\n", e.getMessage());
                            sendErrorReadingMessage(event);
                            return;
                        }
                        try {
                            GuildUploadHelper guildUploadHelperForFile = new GuildUploadHelper(textFromFile, cta_ID, event.getGuild().getIdLong(), true);
                            guildUploadHelperForFile.updateGuildUploadUsers(cta_ID, event);
                            finalTextFile.delete();
                        } catch (Exception e) {
                            System.out.printf("General Error updating guild upload users: %s\n", e.getMessage());
                            sendErrorUpdatingDataMessage(event);
                            return;
                        }
                        GuildCheckHelper guildCheckHelper = new GuildCheckHelper(cta_ID, event.getGuild().getIdLong());
                        guildCheckHelper.sendGuildCheckPaginator(event);
                    }
                });
            } else {
                sendNotGuildListMessage(event);
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            System.out.printf("SQL Error occurred while checking guild list data: %s\n", e.getMessage());
            sendErrorCheckingDataMessage(event);
            return;
        } catch (Exception e) {
            System.out.printf("General Error occurred while checking guild list data: %s\n", e.getMessage());
            sendErrorCheckingDataMessage(event);
            return;
        }
    }

    /* Error Messages as Embeds */
    public void sendErrorReadingMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("An error occurred while reading the file").setDescription("An error occurred while reading the file").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public void sendErrorConnectingDatabaseMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("An error occurred while connecting to the database").setDescription("An error occurred while connecting to the database").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public void sendErrorCheckingDataMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("An error occurred while checking the data").setDescription("An error occurred while checking the data").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public void sendNotGuildListMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("Invalid Format").setDescription("The provided is not a Guild List").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public void sendErrorUpdatingDataMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("An error occurred while updating the data").setDescription("An error occurred while updating the Guild List").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public void sendErrorProvidingOptionsMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("Only one option can be provided").setDescription("You can only provide one option").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }
}
