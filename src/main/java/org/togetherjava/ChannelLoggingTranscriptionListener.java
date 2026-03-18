package org.togetherjava;

import java.awt.Color;
import java.time.Instant;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChannelLoggingTranscriptionListener implements TranscriptionListener {
    private static final Logger LOGGER = LogManager.getLogger(ChannelLoggingTranscriptionListener.class);
    private static final String LOGS_CHANNEL = "logs-vc";

    @Override
    public void receiveTranscription(Guild guild, AudioChannel channel, User user, String transcription) {
        TextChannel logsChannel = guild.getTextChannelsByName(LOGS_CHANNEL, true).stream()
                .findFirst()
                .orElse(null);

        if (logsChannel == null) {
            LOGGER.warn("No '{}' channel found in guild '{}'", LOGS_CHANNEL, guild.getName());
            return;
        }

        Member member = guild.getMember(user);
        String displayName = member != null ? member.getEffectiveName() : user.getEffectiveName();
        String avatarUrl = member != null ? member.getEffectiveAvatarUrl() : user.getEffectiveAvatarUrl();

        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(displayName, null, avatarUrl)
                .setDescription(transcription)
                .addField("User", user.getAsMention(), true)
                .addField("Id", user.getId(), true)
                .addField("Channel", channel.getName(), true)
                .setColor(Color.CYAN)
                .setTimestamp(Instant.now());

        logsChannel.sendMessageEmbeds(embed.build()).queue();
    }
}
