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

import static com.me.LootSplit.utils.Messages.sendRequiredRoleNotPresentMessage;

public class GuestSetupCommand implements ISlashCommand {
    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("guestsetup", "Setup a guest account")
                .setGuildOnly(true)
                .addOption(OptionType.STRING, "role", "The role to assign to the guest", true)
                .addOption(OptionType.INTEGER, "duration", "The duration of the role in minutes", true);
    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply(false).queue();
        try {
            Dotenv config = Dotenv.configure().load();
            String allowedRole = config.get("ALLOWED_ROLE_FOR_LOOTSPLIT");
            String role = event.getOption("role").getAsString();
            Integer duration = event.getOption("duration").getAsInt();
            DatabaseManager databaseManager = new DatabaseManager();

            // Only allow the command to be used by users with the specified role
            if (!event.getMember().getRoles().stream().anyMatch(r -> r.getName().equals(allowedRole))) {
                sendRequiredRoleNotPresentMessage(event);
                return;
            }
            databaseManager.addNewRole(role, duration, Long.parseLong(event.getGuild().getId()));
            sendSuccessMessage(event, "Role added successfully");
            System.out.printf("Added role %s with duration %d to guild %s\n", role, duration, event.getGuild().getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Messages
    private void sendSuccessMessage(@NotNull SlashCommandInteractionEvent event, String message) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Success");
        embedBuilder.setDescription(message);
        embedBuilder.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

}
