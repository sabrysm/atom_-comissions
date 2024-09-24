package com.me.GuildBot;

import com.me.GuildBot.commands.CTACommands;
import com.me.GuildBot.events.GeneralEvents;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;


public class Main {
    private static Dotenv config;
    private final JDA jda;

    private Main() {
        config = Dotenv.configure().load();
        final GeneralEvents generalEvents = new GeneralEvents();
        final CTACommands ctaCommands = new CTACommands();
        final SlashCommandManager slashCommandManager = new SlashCommandManager();
        slashCommandManager.addCommands(ctaCommands);

        jda = JDABuilder.createLight(config.get("TOKEN"))
                .addEventListeners(generalEvents)
                .addEventListeners(slashCommandManager)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();
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
