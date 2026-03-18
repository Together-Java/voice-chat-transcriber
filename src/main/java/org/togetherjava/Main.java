package org.togetherjava;

import java.nio.file.Path;
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
import org.togetherjava.config.Config;
import org.togetherjava.config.ConfigLoader;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    void main() throws Exception {
        Config config = ConfigLoader.loadConfig(Path.of("config.json"));

        VoskTranscriber voskTranscriber = new VoskTranscriber(System.getenv().getOrDefault("VOSK_MODEL_PATH", "model"));

        Set<Long> channels = ConcurrentHashMap.newKeySet();

        int numberOfBotsStarted = 0;

        for (String token : config.botTokens()) {
            if (!token.isBlank()) {
                startBot(config, token, voskTranscriber, channels);
                numberOfBotsStarted++;
            }
        }

        LOGGER.info("Started {} transcription bots", numberOfBotsStarted);
    }

    private static void startBot(
            Config config, String token, VoskTranscriber voskTranscriber, Set<Long> claimedChannels) {
        JDABuilder.createLight(token, GatewayIntent.GUILD_VOICE_STATES)
                .enableCache(CacheFlag.VOICE_STATE)
                .setMemberCachePolicy(MemberCachePolicy.VOICE)
                .setAudioModuleConfig(new AudioModuleConfig().withDaveSessionFactory(new JDaveSessionFactory()))
                .addEventListeners(new VoiceChannelListener(config, voskTranscriber, claimedChannels))
                .addEventListeners(new ListenerAdapter() {
                    @Override
                    public void onReady(@NotNull ReadyEvent event) {
                        new LogsChannelCleanupTask(event.getJDA(), config).start();
                    }
                })
                .build();
    }
}
