package com.me.GuildBot;

import com.me.GuildBot.commands.ISlashCommand;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SlashCommandManager extends ListenerAdapter {
    private final Map<String, ISlashCommand> commands = new HashMap<>();
    private boolean updated = false;

    // Adds multiple commands to the map
    public void addCommands(ISlashCommand... slashCommands) {
        for (ISlashCommand slashCommand : slashCommands) {
            final CommandData commandData = slashCommand.getCommandData(); // Associate the command's name to the slash command object
            commands.put(commandData.getName(), slashCommand);
        }
    }

    // Listens for a GuildReadyEvent and then updates the global commands
    // This will run multiple times, so we need to make sure it only runs once
    @SubscribeEvent
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        if (updated) return; // Return if already updated
        updated = true;

        // Add the commands on the global scope
        event.getJDA().updateCommands()
                .addCommands(commands.values().stream().map(ISlashCommand::getCommandData).toList())
                .queue(response -> System.out.println("Registered " + response.size() + " commands"));
    }

    @SubscribeEvent
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        // Get our slash command by name
        final ISlashCommand slashCommand = commands.get(event.getName());
        if (slashCommand == null) {
            event.reply("This command was not found")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // The slash command exists with such name, execute it
        slashCommand.execute(event);
    }
}