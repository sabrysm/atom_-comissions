package com.me.LootSplit;

import com.me.LootSplit.commands.*;
import com.me.LootSplit.events.GeneralEvents;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;


public class Main {
    private static Dotenv config;
    private final JDA jda;

    private Main() {
        config = Dotenv.configure().load();
        final GeneralEvents generalEvents = new GeneralEvents();
        final SlashCommandManager slashCommandManager = getSlashCommandManager();

        jda = JDABuilder.createLight(config.get("TOKEN"))
                .addEventListeners(generalEvents)
                .addEventListeners(slashCommandManager)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();
    }

    @NotNull
    private static SlashCommandManager getSlashCommandManager() {
        final LootSplitCommands lootSplitCommands = new LootSplitCommands();
        final GuildCommands guildCommands = new GuildCommands();
        final GiveUserAmountCommand giveUserAmountCommand = new GiveUserAmountCommand();
        final RemoveUserAmountCommand removeUserAmountCommand = new RemoveUserAmountCommand();
        final RegisterCommand registerCommand = new RegisterCommand();
        final BalanceCommand balanceCommand = new BalanceCommand();
        final GuestSetupCommand guestSetupCommand = new GuestSetupCommand();
        final LeaderboardCommand leaderboardCommand = new LeaderboardCommand();
        final SlashCommandManager slashCommandManager = new SlashCommandManager();
        slashCommandManager.addCommands(lootSplitCommands, giveUserAmountCommand, removeUserAmountCommand, balanceCommand, guildCommands, guestSetupCommand, registerCommand, leaderboardCommand);
        return slashCommandManager;
    }

    public Dotenv getConfig() {
        return config;
    }

    @NotNull
    public JDA getJDA() {
        return jda;
    }

    public static void main(String[] args) {
        new Main();
    }
}
