package org.red5.io.moq.loc.model;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Audio Level LOC Header Extension.
 *
 * The magnitude of the audio level of the corresponding audio frame as well as
 * a voice activity indicator as defined in RFC6464 Section 3, encoded in the
 * least significant 8 bits of a varint.
 *
 * ID: 6
 * Value Type: Varint (even ID)
 * Length: Varies (1-2 bytes)
 *
 * Bit layout (least significant bits of varint):
 * - Bit 0: V (Voice Activity, 1 = voice, 0 = silence)
 * - Bits 1-7: Level (Audio level, 0-127, 0 = loudest, 127 = silence)
 *
 * Reference: draft-ietf-moq-loc Section 2.3.3.1
 *            RFC6464 Section 3
 */
public class AudioLevelExtension extends LocHeaderExtension {

    public static final int EXTENSION_ID = 6;

    private boolean voiceActivity;
    private int audioLevel; // 0-127, 0 = loudest, 127 = silence

    public AudioLevelExtension() {
        super(EXTENSION_ID);
    }

    public AudioLevelExtension(boolean voiceActivity, int audioLevel) {
        super(EXTENSION_ID);
        this.voiceActivity = voiceActivity;
        setAudioLevel(audioLevel);
    }

    public boolean isVoiceActivity() {
        return voiceActivity;
    }

    public void setVoiceActivity(boolean voiceActivity) {
        this.voiceActivity = voiceActivity;
    }

    public int getAudioLevel() {
        return audioLevel;
    }

    public void setAudioLevel(int audioLevel) {
        if (audioLevel < 0 || audioLevel > 127) {
            throw new IllegalArgumentException("Audio level must be 0-127");
        }
        this.audioLevel = audioLevel;
    }

    @Override
    protected byte[] serializeValue() throws IOException {
        long value = 0;

        if (voiceActivity) value |= 0x01;
        value |= (audioLevel & 0x7F) << 1;

        return serializeVarint(value);
    }

    @Override
    public void deserializeValue(ByteBuffer buffer, int length) throws IOException {
        long value = readVarint(buffer);

        this.voiceActivity = (value & 0x01) != 0;
        this.audioLevel = (int) ((value >> 1) & 0x7F);
    }

    @Override
    public String toString() {
        return "AudioLevelExtension{" +
                "voiceActivity=" + voiceActivity +
                ", audioLevel=" + audioLevel +
                '}';
    }
}
