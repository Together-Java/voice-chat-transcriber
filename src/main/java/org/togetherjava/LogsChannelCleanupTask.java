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
    private final String logsChannelName;
    private final ScheduledExecutorService scheduler;

    public LogsChannelCleanupTask(JDA jda, Config config) {
        this.jda = jda;
        this.logsChannelName = config.logsChannel();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::cleanupAllGuilds, 0, CLEANUP_INTERVAL.toSeconds(), TimeUnit.SECONDS);
    }

    private void cleanupAllGuilds() {
        OffsetDateTime cutoff = OffsetDateTime.now().minus(RETENTION);

        jda.getGuilds().forEach(guild -> guild.getTextChannelsByName(logsChannelName, true).stream()
                .findFirst()
                .ifPresent(channel -> cleanupChannel(channel, cutoff)));
    }

    private void cleanupChannel(TextChannel channel, OffsetDateTime cutoff) {
        fetchAndDeleteBatch(channel.getHistory(), channel, cutoff);
    }

    private void fetchAndDeleteBatch(MessageHistory history, TextChannel channel, OffsetDateTime cutoff) {
        history.retrievePast(BATCH_SIZE)
                .queue(
                        messages -> handleMessages(history, channel, cutoff, messages),
                        error -> logFailure(channel, error));
    }

    private void handleMessages(
            MessageHistory history, TextChannel channel, OffsetDateTime cutoff, List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }

        deleteOldMessages(channel, cutoff, messages);

        if (messages.size() == BATCH_SIZE) {
            fetchAndDeleteBatch(history, channel, cutoff);
        }
    }

    private void deleteOldMessages(TextChannel channel, OffsetDateTime cutoff, List<Message> messages) {
        List<Message> toDelete = messages.stream()
                .filter(msg -> msg.getTimeCreated().isBefore(cutoff))
                .toList();

        if (!toDelete.isEmpty()) {
            channel.purgeMessages(toDelete);
        }
    }

    private void logFailure(TextChannel channel, Throwable error) {
        LOGGER.error("Failed to fetch message history for cleanup in #{}", channel.getName(), error);
    }
}
