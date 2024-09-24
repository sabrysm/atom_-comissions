package com.me.GuildBot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Paginator extends ListenerAdapter {
    private static final Map<String, Paginator> paginators = new HashMap<>();
    private long authorID;
    private static final ReentrantLock lock = new ReentrantLock();
    private static int idSeries = 0;

    private final String id;
    private final List<MessageEmbed> embeds;
    private int currentPage = 0;
    private SlashCommandInteractionEvent event;


    public Paginator(List<MessageEmbed> embeds, Long authorID) {
        lock.lock();
        this.id = Integer.toString(idSeries++);
        lock.unlock();
        this.embeds = embeds;
        paginators.put(id, this);
        this.authorID = authorID;
    }

    private boolean interactionCheck(ButtonInteractionEvent event) {
        // Check if the interaction is from the author of the paginator
        if (event.getUser().getIdLong() != authorID) {
            event.reply("You are not allowed to interact with this paginator.").setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    public void sendInitialMessage(@NotNull SlashCommandInteractionEvent event) {
        event.getHook().sendMessageEmbeds(getCurrentEmbed())
                .setActionRow(getPreviousButton(), getNextButton())
                .queue();
    }

    public MessageEmbed getCurrentEmbed() {
        return embeds.get(currentPage);
    }

    public Button getPreviousButton() {
        Emoji emoji = Emoji.fromUnicode("◀️");
        return Button.primary(id + ":prev", emoji).withDisabled(currentPage == 0);
    }

    public Button getNextButton() {
        Emoji emoji = Emoji.fromUnicode("▶️");
        return Button.primary(id + ":next", emoji).withDisabled(currentPage == embeds.size() - 1);
    }

    public void nextPage() {
        if (currentPage < embeds.size() - 1) {
            currentPage++;
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
        }
    }

    public List<MessageEmbed> createPages() {
        List<MessageEmbed> pages = new ArrayList<>();
        for (int i = 0; i < embeds.size(); i++) {
            EmbedBuilder newPage = new EmbedBuilder();
            newPage.setTitle("Guild List - Page " + (i + 1));
            newPage.setDescription(embeds.get(i).getDescription());
            newPage.setFooter("Page " + (i + 1) + " of " + embeds.size());
            newPage.setColor(embeds.get(i).getColor());
            pages.add(newPage.build());
        }
        return pages;
    }

    private void updatePage(ButtonInteractionEvent event, Paginator paginator) {
        event.editMessageEmbeds(paginator.getCurrentEmbed())
                .setActionRow(paginator.getPreviousButton(), paginator.getNextButton())
                .queue();
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
            default:
                System.out.println("Button not found: " + buttonId);
                return;
        }
        updatePage(event, this);
    }
}