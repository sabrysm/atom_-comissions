package com.me.GuildBot.events;

import com.me.GuildBot.database.DatabaseManager;
import com.me.GuildBot.utils.CTA;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Tasks {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void updateCTAStatus(@NotNull ReadyEvent event) {
        Dotenv config = Dotenv.configure().load();
        Runnable checker = () -> {
            try {
                System.out.println("Checking for finished CTAs");
                DatabaseManager databaseManager = new DatabaseManager();
                List<String> ActiveCTAs = databaseManager.getJustFinishedActiveCTAs();
                for (String ctaID : ActiveCTAs) {
                    // Get End Time for the CTA
                    Instant endTime = databaseManager.getCTAEndTime(ctaID, null);
                    // if end time is before current time, update status to finished
                    if (endTime.isBefore(Instant.now())) {
                        databaseManager.updateCTAStatus(ctaID, "finished");
                        System.out.println("CTA: " + ctaID + " has finished");
                        // get guild ID where this event is happening
                        List<Guild> guildID = event.getJDA().getGuilds();
                        // send to system channel to every server
                        for (Guild guild : guildID) {
                            EmbedBuilder embed = new EmbedBuilder();
                            embed.setTitle("CTA Finished");
                            embed.setDescription("CTA: " + ctaID + " has finished");
                            embed.setColor(0x00FF00);
                            guild.getSystemChannel().sendMessageEmbeds(embed.build()).queue();
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
        scheduler.scheduleAtFixedRate(checker, 0, 10, SECONDS);
    }



    // beep
    public void beep() {
        Runnable beeper = () -> System.out.println("beep");
        scheduler.scheduleAtFixedRate(beeper, 0, 3, SECONDS);
    }

    // main method
    public static void main(String[] args) {
//        Tasks tasks = new Tasks();
//        tasks.updateCTAStatus();
        System.out.println(CTA.getStringFromInstant(Instant.now()));
        }
}
