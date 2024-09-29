package com.me.LootSplit.events;

import com.me.LootSplit.database.DatabaseManager;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class GeneralEvents extends ListenerAdapter {

    // On Ready Event
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println("Bot is ready");
        Tasks tasks = new Tasks();
        tasks.checkExistingRoles(event);
    }

    // When a users gets a role
    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        try {
            System.out.println("Role added to user: " + event.getMember().getUser().getName());
            String roleName = event.getRoles().get(0).getName();
            long guildId = Long.parseLong(event.getGuild().getId());
            DatabaseManager databaseManager = new DatabaseManager();
            if (!databaseManager.roleExists(roleName, guildId)) {
                return;
            }
            Integer roleDuration = databaseManager.getRoleDuration(roleName, guildId);
            if (roleDuration != null) {
                databaseManager.givePlayerRole(event.getMember().getIdLong(), guildId, roleName, roleDuration);
                System.out.printf("Role %s added to user %s with duration %d\n", roleName, event.getMember().getUser().getName(), roleDuration);
            }
        } catch (Exception e) {
            System.out.println("Error getting role duration: " + e.getMessage());
        }

    }

}
