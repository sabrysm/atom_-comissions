package com.me.GuildBot.commands;

import com.me.GuildBot.database.DatabaseManager;
import com.me.GuildBot.utils.CTA;
import com.me.GuildBot.utils.Paginator;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.SQLException;
import java.util.List;

public class ListCommand implements ISlashSubCommand {
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        try {
            DatabaseManager manager = new DatabaseManager();
            // Get all the CTAs with info about (ctaID, startTime, endTime, status)
            List<List<String>> ctas = manager.getAllCTAs(event.getGuild().getIdLong());
            List<MessageEmbed> ctasListPaginator = CTA.getCTAsListPaginator(ctas);
            Paginator ctasPaginator = new Paginator(ctasListPaginator, event.getMember().getIdLong());
            event.getJDA().addEventListener(ctasPaginator);
            ctasPaginator.sendInitialMessage(event);
        } catch (SQLException e) {
            System.out.printf("An error occurred while listing the CTAs in List Command: %s\n", e.getMessage());
            sendErrorListingCTAsMessage(event);
            return;
        }
    }

    /* Error Messages */

    public void sendErrorListingCTAsMessage(SlashCommandInteractionEvent event) {
        event.getHook().sendMessage("An error occurred while listing the CTAs").setEphemeral(true).queue();
    }
}
