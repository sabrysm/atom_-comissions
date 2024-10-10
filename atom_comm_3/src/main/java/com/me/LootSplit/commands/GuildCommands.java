package com.me.LootSplit.commands;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.jetbrains.annotations.NotNull;

import static com.me.LootSplit.utils.Messages.sendRequiredRoleNotPresentMessage;

public class GuildCommands implements ISlashCommand {
    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("guild", "Guild commands")
                .setGuildOnly(true)
                .addSubcommands(
                        new SubcommandData("upload", "Uploads the guild list, which can be used for cross-referencing.")
                                .addOption(OptionType.STRING, "guild_name", "The name of the guild", true)
                                .addOption(OptionType.STRING, "text", "The text that has Guild Names", false)
                                .addOption(OptionType.ATTACHMENT, "file", "The text file that has Guild Names", false)
                )
                .addSubcommands(
                        new SubcommandData("remove", "Removes the guild list.")
                                .addOption(OptionType.STRING, "guild_name", "The name of the guild", true)
                )
                .addSubcommandGroups(
                        new SubcommandGroupData("remove", "Remove a user from the guild list")
                                .addSubcommands(
                                        new SubcommandData("user", "Remove a user from the guild list")
                                                .addOption(OptionType.USER, "user", "The user to remove from the guild list", true)
                                )
                )
                .addSubcommandGroups(
                        new SubcommandGroupData("add", "Add a user to the guild list")
                                .addSubcommands(
                                        new SubcommandData("user", "Add a user to the guild list")
                                                .addOption(OptionType.USER, "user", "The user to add to the guild list", true)
                                )
                );
    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply(false).queue();
        Dotenv config = Dotenv.configure().load();
        String allowedRole = config.get("ALLOWED_ROLE_FOR_LOOTSPLIT");

        // Only allow the command to be used by users with the specified role
        if (!event.getMember().getRoles().stream().anyMatch(r -> r.getName().equals(allowedRole))) {
            sendRequiredRoleNotPresentMessage(event);
            return;
        }

        switch (event.getFullCommandName()) {
            case "guild upload":
                GuildUploadCommand guildUploadCommand = new GuildUploadCommand();
                guildUploadCommand.execute(event);
                break;
            case "guild remove":
                GuildRemoveCommand guildRemoveCommand = new GuildRemoveCommand();
                guildRemoveCommand.execute(event);
                break;
            case "guild remove user":
                GuildRemoveUserCommand guildRemoveUserCommand = new GuildRemoveUserCommand();
                guildRemoveUserCommand.execute(event);
                break;
            case "guild add user":
                GuildAddUserCommand guildAddUserCommand = new GuildAddUserCommand();
                guildAddUserCommand.execute(event);
                break;
        }
    }

}
