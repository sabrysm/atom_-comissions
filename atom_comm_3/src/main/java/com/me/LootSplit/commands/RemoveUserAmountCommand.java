package com.me.LootSplit.commands;

import com.me.LootSplit.database.DatabaseManager;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.me.LootSplit.utils.Messages.*;

public class RemoveUserAmountCommand implements ISlashCommand {
    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("remove", "Removes a specified amount from a user’s balance.")
                .setGuildOnly(true)
                .addOption(OptionType.STRING, "username", "The player name", true)
                .addOption(OptionType.NUMBER, "amount", "The amount to remove", true);

    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        Dotenv config = Dotenv.configure().load();
        String allowedRole = config.get("ALLOWED_ROLE_FOR_LOOTSPLIT");

        event.deferReply(false).queue();

        final String playerName = event.getOption("username").getAsString();
        final double amount = event.getOption("amount").getAsDouble();
        try {
            // Only allow the command to be used by users with the specified role
            if (!event.getMember().getRoles().stream().anyMatch(r -> r.getName().equals(allowedRole))) {
                sendRequiredRoleNotPresentMessage(event);
                return;
            }
            DatabaseManager databaseManager = new DatabaseManager();
            databaseManager.removeUserAmount(playerName, amount);
            sendAmountRemovedFromPlayer(event, playerName, amount);
        } catch (SQLException e) {
            raiseSQLError("Error giving amount to player: " + e.getMessage());
        } catch (Exception e) {
            raiseUnknownError("Error giving amount to player: " + e.getMessage());
        }
    }

}
