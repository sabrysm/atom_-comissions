package com.me.LootSplit.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.jetbrains.annotations.NotNull;

public class LootSplitCommands implements ISlashCommand {

    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("lootsplit", "Main Loot Split command")
                .setGuildOnly(true)
                .addSubcommands(
                        new SubcommandData("create", "Creates a new LootSplit session with the specified name.")
                                .addOption(OptionType.STRING, "name", "The name of the LootSplit session", true)
                )
                .addSubcommandGroups(
                        new SubcommandGroupData("guild", "Guild related commands")

                                .addSubcommands(
                                        new SubcommandData("upload", "Uploads the Loot Split for the guild")
                                                .addOption(OptionType.STRING, "text", "The text that has Guild Names", false)
                                                .addOption(OptionType.ATTACHMENT, "file", "The text file that has Guild Names", false)
                                )
                )
                .addSubcommands(
                        new SubcommandData("add", "Adds a player to the current LootSplit session")
                                .addOption(OptionType.STRING, "username", "The player name", true)
                )
                .addSubcommands(
                        new SubcommandData("remove", "Removes a player from the current LootSplit session")
                                .addOption(OptionType.STRING, "username", "The player name", true)
                )
                .addSubcommands(
                        new SubcommandData("set", "Sets the value for silver and items to be split")
                                .addOption(OptionType.INTEGER, "silver", "The amount of silver to split", true)
                                .addOption(OptionType.INTEGER, "items", "The amount of items to split", true)
                )
                .addSubcommands(
                        new SubcommandData("half", "Player only gets half of the amount added to their balance")
                                .addOption(OptionType.STRING, "username", "The player name", true)
                )
                .addSubcommands(
                        new SubcommandData("confirm", "Confirms the LootSplit with the specified ID")
                                .addOption(OptionType.STRING, "split_id", "The split ID", true)
                )
               .addSubcommandGroups(
                        new SubcommandGroupData("party", "Party related commands")

                                .addSubcommands(
                                        new SubcommandData("upload", "Uploads the loot split for the party")
                                                .addOption(OptionType.INTEGER, "party_number", "The party number", true)
                                                .addOption(OptionType.ATTACHMENT, "party_image", "The image to upload", true)
                                )

                );
    }

    public void execute(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getFullCommandName()) {
            case "lootsplit create":
                event.deferReply(false).queue();
                LootSplitCreateCommand createCommand = new LootSplitCreateCommand();
                createCommand.execute(event);
                break;
            case "lootsplit guild upload":
                event.deferReply(false).queue();
                LootSplitGuildUploadCommand guildUploadCommand = new LootSplitGuildUploadCommand();
                guildUploadCommand.execute(event);
                break;
            case "lootsplit party upload":
                event.deferReply(false).queue();
                LootSplitPartyUploadCommand lootSplitPartyUploadCommand = new LootSplitPartyUploadCommand();
                lootSplitPartyUploadCommand.execute(event);
                break;
            case "lootsplit add":
                event.deferReply(false).queue();
                LootSplitAddCommand lootSplitAddCommand = new LootSplitAddCommand();
                lootSplitAddCommand.execute(event);
                break;
            case "lootsplit remove":
                event.deferReply(false).queue();
                LootSplitRemoveCommand lootSplitRemoveCommand = new LootSplitRemoveCommand();
                lootSplitRemoveCommand.execute(event);
                break;
            case "lootsplit set":
                event.deferReply(false).queue();
                LootSplitSetCommand lootSplitSetCommand = new LootSplitSetCommand();
                lootSplitSetCommand.execute(event);
                break;
            case "lootsplit half":
                event.deferReply(false).queue();
                LootSplitHalfCommand halfCommand = new LootSplitHalfCommand();
                halfCommand.execute(event);
                break;
            case "lootsplit confirm":
                event.deferReply(false).queue();
                LootSplitConfirmCommand confirmCommand = new LootSplitConfirmCommand();
                confirmCommand.execute(event);
                break;
        }
    }
}
