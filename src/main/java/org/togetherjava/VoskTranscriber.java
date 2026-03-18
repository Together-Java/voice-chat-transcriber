package org.togetherjava;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vosk.Model;
import org.vosk.Recognizer;

public class VoskTranscriber {
    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]*)\"");
    private static final int VOSK_SAMPLE_RATE = 16_000;
    private final Model model;

    public VoskTranscriber(String modelPath) throws IOException {
        this.model = new Model(modelPath);
    }

    public Recognizer createRecognizer() {
        try {
            return new Recognizer(model, VOSK_SAMPLE_RATE);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create Vosk recognizer", e);
        }
    }

    /**
     * Converts 48 kHz stereo big-endian PCM to 16 kHz mono little-endian PCM.
     *
     * <p>Discord audio: 48 kHz, stereo, 16-bit signed big-endian PCM. Vosk expects: 16 kHz, mono,
     * 16-bit signed little-endian PCM.
     */
    public static byte[] toVoskFormat(byte[] input) {
        final int BYTES_PER_STEREO_SAMPLE = 4;
        final int DOWNSAMPLE_FACTOR = 3;
        final int BYTES_PER_MONO_SAMPLE = 2;

        int totalStereoSamples = input.length / BYTES_PER_STEREO_SAMPLE;
        int outputSamples = totalStereoSamples / DOWNSAMPLE_FACTOR;
        byte[] output = new byte[outputSamples * BYTES_PER_MONO_SAMPLE];

        for (int outIndex = 0; outIndex < outputSamples; outIndex++) {
            long accumulatedMono = 0;

            for (int i = 0; i < DOWNSAMPLE_FACTOR; i++) {
                int baseOffset = (outIndex * DOWNSAMPLE_FACTOR + i) * BYTES_PER_STEREO_SAMPLE;
                short left = readBigEndianShort(input, baseOffset);
                short right = readBigEndianShort(input, baseOffset + 2);
                short monoSample = (short) ((left + right) / 2);
                accumulatedMono += monoSample;
            }

            short finalSample = (short) (accumulatedMono / DOWNSAMPLE_FACTOR);
            int outOffset = outIndex * BYTES_PER_MONO_SAMPLE;
            writeLittleEndianShort(output, outOffset, finalSample);
        }

        return output;
    }

    public static String extractText(String json) {
        Matcher m = TEXT_PATTERN.matcher(json);
        return m.find() ? m.group(1).trim() : "";
    }

    private static short readBigEndianShort(byte[] data, int offset) {
        return (short) ((data[offset] << 8) | (data[offset + 1] & 0xFF));
    }

    private static void writeLittleEndianShort(byte[] data, int offset, short value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }
}
