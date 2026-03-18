package org.togetherjava;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.togetherjava.config.Config;

public class VoiceChannelListener extends ListenerAdapter {
    private static final Logger LOGGER = LogManager.getLogger(VoiceChannelListener.class);
    private final Config config;
    private final VoskTranscriber voskTranscriber;
    private final Map<Long, AudioHandler> activeHandlers;
    private final List<TranscriptionListener> transcriptionListeners;
    private final Set<Long> claimedChannels;

    public VoiceChannelListener(Config config, VoskTranscriber voskTranscriber, Set<Long> claimedChannels) {
        this.config = config;
        this.voskTranscriber = voskTranscriber;
        this.activeHandlers = new ConcurrentHashMap<>();
        this.transcriptionListeners = List.of(new ChannelLoggingTranscriptionListener(config));
        this.claimedChannels = claimedChannels;
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        AudioChannel joinedChannel = event.getChannelJoined();
        AudioChannel leftChannel = event.getChannelLeft();

        if (event.getMember().equals(guild.getSelfMember())) {
            return;
        }

        if (joinedChannel != null) {
            handleVoiceChannelJoinEvent(guild, event.getMember(), joinedChannel, audioManager);
        }

        if (leftChannel != null) {
            handleVoiceChannelLeaveEvent(guild, event.getMember(), leftChannel, audioManager);
        }
    }

    private void handleVoiceChannelJoinEvent(
            Guild guild, Member member, AudioChannel channel, AudioManager audioManager) {
        LOGGER.debug("{} joined {}", member.getEffectiveName(), channel.getName());

        if (shouldJoinVoiceChannel(channel, audioManager)) {
            LOGGER.info("Joining voice channel {}", channel.getName());
            joinVoiceChannel(guild, channel, audioManager);
        }
    }

    private void handleVoiceChannelLeaveEvent(
            Guild guild, Member member, AudioChannel channel, AudioManager audioManager) {
        LOGGER.debug("{} left {}", member.getEffectiveName(), channel.getName());

        if (isVoiceChannelEmpty(channel) && isInVoiceChannel(channel, audioManager)) {
            LOGGER.info("Voice channel {} has no more participants, leaving...", channel.getName());
            leaveVoiceChannel(guild, channel, audioManager);
        }
    }

    private void joinVoiceChannel(Guild guild, AudioChannel channel, AudioManager audioManager) {
        AudioHandler handler = new AudioHandler(guild, channel, voskTranscriber, transcriptionListeners);
        activeHandlers.put(guild.getIdLong(), handler);
        audioManager.setReceivingHandler(handler);
        audioManager.openAudioConnection(channel);
    }

    private void leaveVoiceChannel(Guild guild, AudioChannel channel, AudioManager audioManager) {
        claimedChannels.remove(channel.getIdLong());
        audioManager.setReceivingHandler(null);
        audioManager.closeAudioConnection();
        AudioHandler handler = activeHandlers.remove(guild.getIdLong());
        if (handler != null) {
            handler.finish();
        }
    }

    private boolean isInVoiceChannel(AudioManager audioManager) {
        return audioManager.getConnectedChannel() != null;
    }

    private boolean isInVoiceChannel(AudioChannel channel, AudioManager audioManager) {
        return channel.equals(audioManager.getConnectedChannel());
    }

    private boolean shouldJoinVoiceChannel(AudioChannel joinedChannel, AudioManager audioManager) {
        return !isInVoiceChannel(audioManager)
                && !config.ignoreChannels().contains(joinedChannel.getName())
                && claimedChannels.add(joinedChannel.getIdLong());
    }

    private boolean isVoiceChannelEmpty(AudioChannel channel) {
        return channel.getMembers().stream().allMatch(m -> m.getUser().isBot());
    }
}
