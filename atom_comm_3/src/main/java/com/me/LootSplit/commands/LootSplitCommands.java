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
               .addSubcommandGroups(
                        new SubcommandGroupData("party", "Party related commands")

                                .addSubcommands(
                                        new SubcommandData("upload", "Uploads the loot split for the party")
                                                .addOption(OptionType.ATTACHMENT, "party_image", "The image to upload", true)
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
                );
    }

    public void execute(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply(false).queue();
        switch (event.getFullCommandName()) {
            case "lootsplit create":
                LootSplitCreateCommand createCommand = new LootSplitCreateCommand();
                createCommand.execute(event);
                break;
            case "lootsplit guild upload":
                LootSplitGuildUploadCommand guildUploadCommand = new LootSplitGuildUploadCommand();
                guildUploadCommand.execute(event);
                break;
            case "lootsplit party upload":
                LootSplitPartyUploadCommand lootSplitPartyUploadCommand = new LootSplitPartyUploadCommand();
                lootSplitPartyUploadCommand.execute(event);
                break;
            case "lootsplit add":
                LootSplitAddCommand lootSplitAddCommand = new LootSplitAddCommand();
                lootSplitAddCommand.execute(event);
                break;
            case "lootsplit remove":
                LootSplitRemoveCommand lootSplitRemoveCommand = new LootSplitRemoveCommand();
                lootSplitRemoveCommand.execute(event);
                break;
            case "lootsplit set":
                LootSplitSetCommand lootSplitSetCommand = new LootSplitSetCommand();
                lootSplitSetCommand.execute(event);
                break;
            case "lootsplit half":
                LootSplitHalfBalanceCommand halfCommand = new LootSplitHalfBalanceCommand();
                halfCommand.execute(event);
                break;
            case "lootsplit confirm":
                LootSplitConfirmCommand confirmCommand = new LootSplitConfirmCommand();
                confirmCommand.execute(event);
                break;
        }
    }
}
