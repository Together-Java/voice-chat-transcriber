package org.togetherjava;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.togetherjava.config.Config;

public class LogsChannelCleanupTask {
    private static final Logger LOGGER = LogManager.getLogger(LogsChannelCleanupTask.class);
    private static final Duration RETENTION = Duration.ofDays(7);
    private static final Duration CLEANUP_INTERVAL = Duration.ofDays(1);
    private static final int BATCH_SIZE = 100;
    private final JDA jda;
    private final String logsChannel;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public LogsChannelCleanupTask(JDA jda, Config config) {
        this.jda = jda;
        this.logsChannel = config.logsChannel();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::cleanGuild, 0, CLEANUP_INTERVAL.toSeconds(), TimeUnit.SECONDS);
    }

    private void cleanGuild() {
        jda.getGuilds().forEach(guild -> guild.getTextChannelsByName(logsChannel, true).stream()
                .findFirst()
                .ifPresent(this::cleanChannel));
    }

    private void cleanChannel(TextChannel channel) {
        fetchAndDelete(channel, channel.getHistory(), OffsetDateTime.now().minus(RETENTION));
    }

    private void fetchAndDelete(TextChannel channel, MessageHistory history, OffsetDateTime cutoff) {
        history.retrievePast(BATCH_SIZE)
                .queue(
                        messages -> {
                            if (messages.isEmpty()) {
                                return;
                            }

                            List<Message> messagesToDelete = messages.stream()
                                    .filter(message -> message.getTimeCreated().isBefore(cutoff))
                                    .toList();

                            if (!messagesToDelete.isEmpty()) {
                                channel.purgeMessages(messagesToDelete);
                            }

                            if (messages.size() == BATCH_SIZE) {
                                fetchAndDelete(channel, history, cutoff);
                            }
                        },
                        e -> LOGGER.error("Failed to fetch message history for cleanup in #{}", channel.getName(), e));
    }
}
