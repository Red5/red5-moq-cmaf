package org.red5.io.moq.loc.model;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Capture Timestamp LOC Header Extension.
 *
 * Wall-clock time in microseconds since the Unix epoch when the encoded media
 * frame was captured, encoded as a varint.
 *
 * ID: 2
 * Value Type: Varint (even ID)
 * Length: Varies (1-8 bytes)
 *
 * Reference: draft-ietf-moq-loc Section 2.3.1.1
 */
public class CaptureTimestampExtension extends LocHeaderExtension {

    public static final int EXTENSION_ID = 2;

    private long captureTimestampMicros;

    public CaptureTimestampExtension() {
        super(EXTENSION_ID);
    }

    public CaptureTimestampExtension(long captureTimestampMicros) {
        super(EXTENSION_ID);
        this.captureTimestampMicros = captureTimestampMicros;
    }

    public long getCaptureTimestampMicros() {
        return captureTimestampMicros;
    }

    public void setCaptureTimestampMicros(long captureTimestampMicros) {
        this.captureTimestampMicros = captureTimestampMicros;
    }

    @Override
    protected byte[] serializeValue() throws IOException {
        return serializeVarint(captureTimestampMicros);
    }

    @Override
    public void deserializeValue(ByteBuffer buffer, int length) throws IOException {
        this.captureTimestampMicros = readVarint(buffer);
    }

    @Override
    public String toString() {
        return "CaptureTimestampExtension{" +
                "captureTimestampMicros=" + captureTimestampMicros +
                '}';
    }
}
