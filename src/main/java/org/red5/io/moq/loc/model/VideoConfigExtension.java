package org.red5.io.moq.loc.model;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Video Config LOC Header Extension.
 *
 * Video codec configuration "extradata", as defined by the corresponding codec
 * specification, which maps to the WebCodecs VideoDecoderConfig description
 * property in the EncodedVideoChunkMetadata.
 *
 * For H.264/AVC and H.265/HEVC, this contains parameter sets (SPS, PPS, VPS)
 * for "canonical" formats like "avc1" and "hvc1" codec strings.
 *
 * ID: 13
 * Value Type: Byte array (odd ID)
 * Length: Varies
 *
 * Reference: draft-ietf-moq-loc Section 2.3.2.1
 */
public class VideoConfigExtension extends LocHeaderExtension {

    public static final int EXTENSION_ID = 13;

    private byte[] configData;

    public VideoConfigExtension() {
        super(EXTENSION_ID);
    }

    public VideoConfigExtension(byte[] configData) {
        super(EXTENSION_ID);
        this.configData = configData;
    }

    public byte[] getConfigData() {
        return configData;
    }

    public void setConfigData(byte[] configData) {
        this.configData = configData;
    }

    @Override
    protected byte[] serializeValue() throws IOException {
        return configData != null ? configData : new byte[0];
    }

    @Override
    public void deserializeValue(ByteBuffer buffer, int length) throws IOException {
        this.configData = new byte[length];
        buffer.get(this.configData);
    }

    @Override
    public String toString() {
        return "VideoConfigExtension{" +
                "configDataLength=" + (configData != null ? configData.length : 0) +
                '}';
    }
}
