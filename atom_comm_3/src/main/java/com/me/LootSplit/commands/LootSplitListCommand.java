package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import com.me.LootSplit.utils.LootSplitSession;
import com.me.LootSplit.utils.Paginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.collections4.OrderedMap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.me.LootSplit.utils.Messages.*;

public class LootSplitListCommand implements ISlashSubCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        try {
            DatabaseManager databaseManager = new DatabaseManager();
            Long guildId = event.getGuild().getIdLong();
            List<OrderedMap<String, String>> lootSplitSessions = databaseManager.getLootSplitSessions(guildId);
            if (lootSplitSessions.isEmpty()) {
                sendNoLootSplitSessionsMessage(event);
                return;
            }
            sendLootSplitSessionsListMessage(event, lootSplitSessions);
        } catch (SQLException e) {
            raiseSQLError("Error setting LootSplit: " + e.getMessage());
        } catch (Exception e) {
            raiseUnknownError("Error setting LootSplit: " + e.getMessage());
        }
    }

    private void sendNoLootSplitSessionsMessage(SlashCommandInteractionEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("No Sessions");
        embedBuilder.setDescription("There is no LootSplit session in this server.");
        embedBuilder.setColor(0xFF0000);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    private void sendLootSplitSessionsListMessage(SlashCommandInteractionEvent event, List<OrderedMap<String, String>> lootSplitSessions) {
        List<MessageEmbed> messageEmbeds = createMessageEmbeds(event, lootSplitSessions);
        Paginator paginator = new Paginator(messageEmbeds, event.getUser().getIdLong());
        event.getJDA().addEventListener(paginator);
        paginator.sendInitialMessage(event);
    }

    private List<MessageEmbed> createMessageEmbeds(SlashCommandInteractionEvent event, List<OrderedMap<String, String>> lootSplitSessions) {
        /*
         * Each embed has 5 fields, so we need to create a new embed every 5 sessions
         * */
        List<MessageEmbed> messageEmbeds = new ArrayList<>();
        int fieldsPerEmbed = 5;
        int pages = (int) Math.ceil(lootSplitSessions.size() / (double) fieldsPerEmbed);
        for (int i = 0; i < pages; i++) {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("LootSplit Sessions");
            for (int j = i * fieldsPerEmbed; j < Math.min((i + 1) * fieldsPerEmbed, lootSplitSessions.size()); j++) {
                OrderedMap<String, String> lootSplitSession = lootSplitSessions.get(j);
                embedBuilder.addField(lootSplitSession.get("name"), "- **Status:** `" + toTitleCase(lootSplitSession.get("status"))+ "`\n- **Split ID:** `" + lootSplitSession.get("split_id") + "`", false);
            }
            embedBuilder.setColor(0x00FF00);
            messageEmbeds.add(embedBuilder.build());
        }
        return messageEmbeds;
    }

    private String toTitleCase(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}