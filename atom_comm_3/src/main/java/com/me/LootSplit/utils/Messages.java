package com.me.LootSplit.utils;

import com.me.LootSplit.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;


public class Messages {
    // sendErrorMessage(embedTitle, embedDescription, color, event);
    public static void sendMessage(String embedTitle, String embedDescription, int color, SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(embedTitle)
                .setDescription(embedDescription)
                .setColor(color);

        event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
    }

    public static void sendNoLootSplitActiveMessage(SlashCommandInteractionEvent event) {
        sendMessage("No LootSplit", "No LootSplit is currently active for this server", 0xFF0000, event);
    }

    public static void sendAmountAddedToPlayer(SlashCommandInteractionEvent event, String playerName, double amount) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Amount Added");
        embedBuilder.setDescription("Amount of " + amount + " has been added to **" + playerName + "**'s balance");
        embedBuilder.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void sendAmountRemovedFromPlayer(SlashCommandInteractionEvent event, String playerName, double amount) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Amount Removed");
        embedBuilder.setDescription("Amount of " + amount + " has been removed from **" + playerName + "**'s balance");
        embedBuilder.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void sendAmountHalvedFromPlayer(SlashCommandInteractionEvent event, String playerName) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Amount Halved");
        embedBuilder.setDescription("Amount has been halved from **" + playerName + "**'s balance");
        embedBuilder.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void sendUserNotInGuildListMessage(SlashCommandInteractionEvent event, String username) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Not Found");
        embedBuilder.setDescription("The username **" + username + "** is not in the Guild List");
        embedBuilder.setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void sendRequiredRoleNotPresentMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Permission Denied");
        embedBuilder.setDescription("You do not have the required role to use this command.");
        embedBuilder.setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void sendPlayerNotInLootSplitMessage(SlashCommandInteractionEvent event, String playerName) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Not Found");
        embedBuilder.setDescription("The player **" + playerName + "** is not in the LootSplit session");
        embedBuilder.setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void sendUserAlreadyExistsMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Already Exists");
        embedBuilder.setDescription("The username you are trying to register already exists");
        embedBuilder.setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void sendUserAlreadyRegisteredMessage(SlashCommandInteractionEvent event, boolean toSelf) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Already Registered");
        if (toSelf)
            embedBuilder.setDescription("You are already registered");
        else
            embedBuilder.setDescription("The user is already registered");
        embedBuilder.setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void sendUserRegisteredSuccessfullyMessage(SlashCommandInteractionEvent event, String username, boolean toSelf) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Registered");
        if (toSelf)
            embedBuilder.setDescription("You have been registered with \nthe username: **" + username + "** successfully");
        else
            embedBuilder.setDescription("The user has been registered with \nthe username: **" + username + "** successfully");
        embedBuilder.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void sendUserUnregisteredSuccessfullyMessage(SlashCommandInteractionEvent event, String username, boolean toSelf) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Unregistered");
        if (toSelf)
            embedBuilder.setDescription("You have been unregistered with \nthe username: **" + username + "** successfully");
        else
            embedBuilder.setDescription("The user has been unregistered with \nthe username: **" + username + "** successfully");
        embedBuilder.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void sendUserNotRegisteredMessage(SlashCommandInteractionEvent event, boolean toSelf) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Not Registered");
        if (toSelf)
            embedBuilder.setDescription("You are not registered. Please register using the `/register` command");
        else
            embedBuilder.setDescription("The user is not registered");
        embedBuilder.setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void raiseSQLError(String message) {
        System.out.printf("SQLException: %s\n", message);
    }

    public static void raiseUnknownError(String message) {
        System.out.printf("Unknown error: %s\n", message);
    }

    public static void sendNoActiveLootSplitSessionMessage(SlashCommandInteractionEvent event) {
        sendMessage("No Active Session", "There is no active LootSplit session for this server\nCreate one using ```/lootsplit create <lootSplitName>```", 0xFF0000, event);
    }

    public static void sendLootSplitSessionAlreadyActiveMessage(SlashCommandInteractionEvent event) {
        sendMessage("Session is Active", "A LootSplit session is already active for this server", 0xFF0000, event);
    }

    public static void sendLootSplitSessionCreatedMessage(SlashCommandInteractionEvent event, String name, String splitId) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Session Created");
        embedBuilder.setDescription("A new LootSplit session has been created");
        embedBuilder.addField("Name", "```" + name + "```", true);
        embedBuilder.addField("SplitID", "```" + splitId + "```", true);
        embedBuilder.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void sendNotGuildListMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("Invalid Format").setDescription("The provided is not a Guild List").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public static void sendErrorUploadMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("An error occurred while uploading the data").setDescription("An error occurred while uploading the data").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public static void sendErrorReadingMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("An error occurred while reading the data").setDescription("An error occurred while reading the data").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public static void sendErrorConnectingDatabaseMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("An error occurred while connecting to the database").setDescription("An error occurred while connecting to the database").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public static void sendErrorOnlyOneOptionAllowedMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("Only one option is allowed").setDescription("Only one option is allowed").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public static void sendErrorProvideTextOptionMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("You need to provide a text option").setDescription("You need to provide a text option").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public static void sendNoValidNamesFoundMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("No Valid Names Found").setDescription("No valid names were found in the image").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public static void sendErrorProcessingImageMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("Error Processing Image").setDescription("There was an error processing the image").setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    public static void sendPlayerAddedToLootSplit(SlashCommandInteractionEvent event, String playerName) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Player Added");
        embedBuilder.setDescription("**" + playerName + "** has been added to the LootSplit session");
        embedBuilder.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void sendPlayerRemovedFromLootSplit(SlashCommandInteractionEvent event, String playerName) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Player Removed");
        embedBuilder.setDescription("**" + playerName + "** has been removed from the LootSplit session");
        embedBuilder.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

}
