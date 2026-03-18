package org.togetherjava;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public interface TranscriptionListener {
    void receiveTranscription(Guild guild, User user, String transcription);
}
