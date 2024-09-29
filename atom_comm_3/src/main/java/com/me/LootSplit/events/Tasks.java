package com.me.LootSplit.events;

import com.me.LootSplit.database.DatabaseManager;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Tasks {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void checkExistingRoles(@NotNull ReadyEvent event) {
        /*
        * - Every min decrement by 1 the users’ “time_left”
        * - users with 0 or less “time_left” gets removed from
        * the `players_roles` table (since he is no longer having a role)
        * */
        Dotenv config = Dotenv.configure().load();
        Runnable checker = () -> {
            try {
                System.out.println("Checking for existing roles");
                DatabaseManager databaseManager = new DatabaseManager();
                List<Guild> guilds = event.getJDA().getGuilds();

                for (Guild guild : guilds) {
                    databaseManager.decrementTimeLeft(Long.parseLong(guild.getId()));
                    List<String> playersWithExpiredRoles = databaseManager.getPlayersWithExpiredRoles(Long.parseLong(guild.getId()));
                    for (String player : playersWithExpiredRoles) {
                        long player_id = Long.parseLong(player.split("\\|\\|\\|")[0]);
                        String roleName = player.split("\\|\\|\\|")[1];
                        System.out.printf("Player with id: %d has expired role: %s\n", player_id, roleName);
                        guild.removeRoleFromMember(UserSnowflake.fromId(player_id), guild.getRolesByName(roleName, true).get(0)).queue();
                        databaseManager.removePlayerRole(player_id, Long.parseLong(guild.getId()));
                        // Sleep for 1 second to avoid rate limiting
                        Thread.sleep(1000);
                    }
                }
            } catch (SQLException e) {
                System.out.println("Error removing players with expired roles: " + e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("Error sleeping thread: " + e.getMessage());
            }
        };
        scheduler.scheduleAtFixedRate(checker, 0, 1, MINUTES);
    }

    // beep
    public void beep() {
        Runnable beeper = () -> System.out.println("beep");
        scheduler.scheduleAtFixedRate(beeper, 0, 3, SECONDS);
    }

    // main method
    public static void main(String[] args) {
//        Tasks tasks = new Tasks();
//        tasks.checkExistingRoles();
        }
}
