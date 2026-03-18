package org.togetherjava;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vosk.Recognizer;

public class AudioHandler implements AudioReceiveHandler {
    private static final Logger LOGGER = LogManager.getLogger(AudioHandler.class);
    private static final long SILENCE_THRESHOLD_MS = 500;

    private final Guild guild;
    private final AudioChannel channel;
    private final VoskTranscriber voskTranscriber;
    private final Map<User, Recognizer> recognizers;
    private final Map<User, Long> lastAudioTime;
    private final List<TranscriptionListener> transcriptionListeners;
    private final ScheduledExecutorService silenceDetector;

    public AudioHandler(
            Guild guild,
            AudioChannel channel,
            VoskTranscriber voskTranscriber,
            List<TranscriptionListener> transcriptionListeners) {
        this.guild = guild;
        this.channel = channel;
        this.voskTranscriber = voskTranscriber;
        this.recognizers = new ConcurrentHashMap<>();
        this.lastAudioTime = new ConcurrentHashMap<>();
        this.transcriptionListeners = transcriptionListeners;
        this.silenceDetector = Executors.newSingleThreadScheduledExecutor();
        silenceDetector.scheduleAtFixedRate(
                this::flushSilentUsers, SILENCE_THRESHOLD_MS, SILENCE_THRESHOLD_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handleUserAudio(UserAudio userAudio) {
        User user = userAudio.getUser();

        Recognizer recognizer = recognizers.computeIfAbsent(user, _ -> voskTranscriber.createRecognizer());
        lastAudioTime.put(user, System.currentTimeMillis());

        byte[] pcm = VoskTranscriber.toVoskFormat(userAudio.getAudioData(1.0));

        synchronized (recognizer) {
            if (recognizer.acceptWaveForm(pcm, pcm.length)) {
                logTranscription(user, VoskTranscriber.extractText(recognizer.getFinalResult()));
            }
        }
    }

    private void flushSilentUsers() {
        long now = System.currentTimeMillis();
        lastAudioTime.forEach((user, lastTime) -> {
            if (now - lastTime > SILENCE_THRESHOLD_MS) {
                lastAudioTime.remove(user);
                Recognizer recognizer = recognizers.get(user);
                if (recognizer != null) {
                    synchronized (recognizer) {
                        logTranscription(user, VoskTranscriber.extractText(recognizer.getFinalResult()));
                    }
                }
            }
        });
    }

    public void finish() {
        silenceDetector.shutdown();
        recognizers.forEach((user, recognizer) -> {
            synchronized (recognizer) {
                logTranscription(user, VoskTranscriber.extractText(recognizer.getFinalResult()));
                recognizer.close();
            }
        });
        recognizers.clear();
    }

    private void logTranscription(User user, String transcription) {
        if (!transcription.isBlank()) {
            transcriptionListeners.forEach(transcriptionListener ->
                    transcriptionListener.receiveTranscription(guild, channel, user, transcription));
        }
    }

    @Override
    public boolean canReceiveUser() {
        return true;
    }
}
