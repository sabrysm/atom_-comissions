package com.me.GuildBot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.jetbrains.annotations.NotNull;

public class CTACommands implements ISlashCommand {

    @NotNull
    @Override
    public CommandData getCommandData() {
        return Commands.slash("cta", "Main CTA command")
                .setGuildOnly(true)
                .addSubcommands(
                        new SubcommandData("start", "Starts a CTA with UTC time")
                                .addOption(OptionType.STRING, "start_time", "The start time of the CTA in UTC", true)
                                .addOption(OptionType.STRING, "end_time", "The end time of the CTA in UTC", true)
                )
                .addSubcommands(
                        new SubcommandData("cancel", "Cancels the current CTA")
                )
                .addSubcommands(
                        new SubcommandData("delete", "Deletes the current CTA")
                )
                .addSubcommands(
                        new SubcommandData("list", "Lists all the CTAs")
                )
                .addSubcommands(
                        new SubcommandData("laterregister", "Adds the user to CTA as a \"LATE/FILL\" section, not as a Party #")
                                .addOption(OptionType.STRING, "username", "The player name", true)
                )
                // stats, addreport, info
                .addSubcommands(
                        new SubcommandData("stats", "See how many attendances/lates a person has")
                                .addOption(OptionType.STRING, "username", "The player name", true)

                )
                .addSubcommands(
                        new SubcommandData("addreport", "Add a report for a CTA")
                                .addOption(OptionType.STRING, "report", "Provide a report for the CTA", true)
                                .addOption(OptionType.STRING, "cta_id", "The CTA ID", false)
                )
                .addSubcommands(
                        new SubcommandData("info", "Get info about a CTA")
                                .addOption(OptionType.STRING, "cta_id", "The CTA ID", true)
                )
                .addSubcommandGroups(
                        new SubcommandGroupData("guild", "Guild related commands")

                                .addSubcommands(
                                        new SubcommandData("upload", "Uploads a guild list")
                                                .addOption(OptionType.STRING, "text", "The text that has Guild Names", false)
                                                .addOption(OptionType.ATTACHMENT, "file", "The text file that has Guild Names", false)
                                        ,
                                        new SubcommandData("check", "Checks the guild list for CTA attendees")
                                                .addOption(OptionType.STRING, "text", "The text that has Guild Names", false)
                                                .addOption(OptionType.ATTACHMENT, "file", "The text file that has Guild Names", false)
                                )

                )
                .addSubcommandGroups(
                        new SubcommandGroupData("party", "Party related commands")

                                .addSubcommands(
                                        new SubcommandData("upload", "Uploads a Party Data from image")
                                                .addOption(OptionType.INTEGER, "party_number", "The party number", true)
                                                .addOption(OptionType.ATTACHMENT, "party_image", "The image to upload", true),

                                        new SubcommandData("add", "Manually add a player to a party")
                                                .addOption(OptionType.INTEGER, "party_number", "The party number", true)
                                                .addOption(OptionType.STRING, "username", "The player name", true),
                                        new SubcommandData("remove", "Manually remove a player from a party")
                                                .addOption(OptionType.INTEGER, "party_number", "The party number", true)
                                                .addOption(OptionType.STRING, "username", "The player name", true)
                                )

                );
    }

    public void execute(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getFullCommandName()) {
            case "cta start":
                event.deferReply(false).queue();
                StartCommand startCommand = new StartCommand();
                startCommand.execute(event);
                break;
            case "cta guild upload":
                event.deferReply(false).queue();
                GuildUploadCommand guildUploadCommand = new GuildUploadCommand();
                guildUploadCommand.execute(event);
                break;
            case "cta guild check":
                event.deferReply(false).queue();
                GuildCheckCommand guildCheckCommand = new GuildCheckCommand();
                guildCheckCommand.execute(event);
                break;
            case "cta party upload":
                event.deferReply(false).queue();
                PartyUploadCommand partyUploadCommand = new PartyUploadCommand();
                partyUploadCommand.execute(event);
                break;
            case "cta party add":
                event.deferReply(true).queue();
                PartyAddCommand partyAddCommand = new PartyAddCommand();
                partyAddCommand.execute(event);
                break;
            case "cta party remove":
                event.deferReply(true).queue();
                PartyRemoveCommand partyRemoveCommand = new PartyRemoveCommand();
                partyRemoveCommand.execute(event);
                break;
            case "cta laterregister":
                event.deferReply(true).queue();
                LateRegisterCommand lateRegisterCommand = new LateRegisterCommand();
                lateRegisterCommand.execute(event);
                break;
            case "cta stats":
                event.deferReply(false).queue();
                StatsCommand statsCommand = new StatsCommand();
                statsCommand.execute(event);
                break;
            case "cta addreport":
                event.deferReply(true).queue();
                AddReportCommand addReportCommand = new AddReportCommand();
                addReportCommand.execute(event);
                break;
            case "cta info":
                event.deferReply(false).queue();
                InfoCommand infoCommand = new InfoCommand();
                infoCommand.execute(event);
                break;
            case "cta cancel":
                event.deferReply(false).queue();
                CancelCommand cancelCommand = new CancelCommand();
                cancelCommand.execute(event);
                break;
            case "cta delete":
                event.deferReply(false).queue();
                DeleteCommand deleteCommand = new DeleteCommand();
                deleteCommand.execute(event);
                break;
            case "cta list":
                event.deferReply(false).queue();
                ListCommand listCommand = new ListCommand();
                listCommand.execute(event);
                break;
        }
    }
}
