package com.me.LootSplit.utils;

import com.me.LootSplit.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class LootSplitSubmissionPaginator extends Paginator {
    private long authorID;
    private long guildID;
    private boolean isSessionFinished = false;
    private String sessionCreator;
    private static final ReentrantLock lock = new ReentrantLock();
    private static int idSeries = 0;

    private final String id;
    private final String splitId;
    private final Double amount;
    private final DatabaseManager databaseManager = new DatabaseManager();
    private final List<MessageEmbed> embeds;
    private int currentPage = 0;
    private SlashCommandInteractionEvent event;


    public LootSplitSubmissionPaginator(List<MessageEmbed> embeds, long authorID, long guildID, String splitId, Double amount) throws SQLException {
        super(embeds, authorID);
        lock.lock();
        this.id = Integer.toString(idSeries++);
        lock.unlock();
        this.splitId = splitId;
        this.amount = amount;
        this.embeds = embeds;
        this.authorID = authorID;
        this.sessionCreator = databaseManager.getLootSplitSessionCreator(guildID);
        paginators.put(id, this);
    }


    private boolean preConfirmCheck(ButtonInteractionEvent event) {
        if (event.getUser().getName().equals(sessionCreator)) {
            return true;
        }
        event.getHook().sendMessage("Only the session creator can confirm the loot split.").queue();
        return false;
    }

    public Button getConfirmButton() {
        Emoji emoji = Emoji.fromUnicode("✅");
        return Button.primary(id + ":confirm", emoji).withDisabled(isSessionFinished);
    }

    public Button getCancelButton() {
        Emoji emoji = Emoji.fromUnicode("❌");
        return Button.primary(id + ":cancel", emoji).withDisabled(isSessionFinished);
    }

    public void sendInitialMessage(@NotNull SlashCommandInteractionEvent event) {
        event.getHook().sendMessageEmbeds(getCurrentEmbed())
                .setActionRow(getPreviousButton(), getConfirmButton(), getCancelButton(), getNextButton())
                .queue();
    }

    private void updatePage(ButtonInteractionEvent event, LootSplitSubmissionPaginator paginator) {
        event.editMessageEmbeds(paginator.getCurrentEmbed())
                .setActionRow(paginator.getPreviousButton(), paginator.getConfirmButton(), paginator.getCancelButton(), paginator.getNextButton())
                .queue();
    }

    public void confirm(ButtonInteractionEvent event) throws SQLException {
        if (!preConfirmCheck(event)) return;
        databaseManager.addBalanceToAllLootSplitPlayers(splitId, amount);
        databaseManager.endLootSplitSession(event.getGuild().getIdLong());
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Loot Split Confirmed");
        embed.setDescription("The loot split session has been confirmed.");
        embed.setColor(0x6064f4);
//        paginators.remove(id);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    public void cancel(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Loot Split Cancelled");
        embed.setDescription("The loot split session has been cancelled.");
        embed.setColor(0xFF0000);
//        paginators.remove(id);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        String[] idParts = buttonId.split(":");
        if (idParts.length != 2) {
            System.out.println("Invalid button id: " + buttonId);
            return;
        }
        String paginatorId = idParts[0];
        String action = idParts[1];

        if (!Objects.equals(paginatorId, this.id)) return;

        if (!interactionCheck(event)) return;


        switch (action) {
            case "next":
                nextPage();
                break;
            case "prev":
                previousPage();
                break;
            case "confirm":
                try {
                    isSessionFinished = true;
                    confirm(event);

                } catch (SQLException e) {
                    System.out.println("Error confirming loot split: " + e.getMessage());
                }
                break;
            case "cancel":
                isSessionFinished = true;
                cancel(event);
                break;
            default:
                System.out.println("Button not found: " + buttonId);
                return;
        }
        updatePage(event, this);
    }
}