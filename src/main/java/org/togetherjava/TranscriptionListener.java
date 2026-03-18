package org.togetherjava;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;

public interface TranscriptionListener {
    void receiveTranscription(Guild guild, AudioChannel channel, User user, String transcription);
}
