package org.togetherjava;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    private static final String VOSK_MODEL_PATH = System.getenv().getOrDefault("VOSK_MODEL_PATH", "model");

    void main() throws Exception {
        String botTokens = System.getenv("BOT_TOKENS");

        if (botTokens == null || botTokens.isBlank()) {
            throw new IllegalStateException("BOT_TOKENS environment variable is not set");
        }

        VoskTranscriber voskTranscriber = new VoskTranscriber(VOSK_MODEL_PATH);
        Set<Long> claimedChannels = ConcurrentHashMap.newKeySet();

        int numberOfBotsStarted = 0;

        for (String token : botTokens.split(",")) {
            if (!token.isBlank()) {
                startBot(token, voskTranscriber, claimedChannels);
                numberOfBotsStarted++;
            }
        }

        LOGGER.info("Started {} transcription bots", numberOfBotsStarted);
    }

    private static void startBot(String token, VoskTranscriber voskTranscriber, Set<Long> claimedChannels) {
        JDABuilder.createLight(token, GatewayIntent.GUILD_VOICE_STATES)
                .enableCache(CacheFlag.VOICE_STATE)
                .setMemberCachePolicy(MemberCachePolicy.VOICE)
                .setAudioModuleConfig(new AudioModuleConfig().withDaveSessionFactory(new JDaveSessionFactory()))
                .addEventListeners(new VoiceChannelListener(voskTranscriber, claimedChannels))
                .addEventListeners(new ListenerAdapter() {
                    @Override
                    public void onReady(@NotNull ReadyEvent event) {
                        new LogsChannelCleanupTask(event.getJDA()).start();
                    }
                })
                .build();
    }
}
