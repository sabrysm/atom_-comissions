package com.me.GuildBot.events;

import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class GeneralEvents extends ListenerAdapter {

    // On Ready Event
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println("Bot is ready");
        Tasks tasks = new Tasks();
        tasks.updateCTAStatus(event);
    }

}
