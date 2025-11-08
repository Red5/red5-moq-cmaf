package org.red5.io.moq.loc.model;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Video Frame Marking LOC Header Extension.
 *
 * Flags for video frames which are independent, discardable, or base layer sync points,
 * as well as temporal and spatial layer identification, as defined in RFC9626.
 *
 * ID: 4
 * Value Type: Varint (even ID)
 * Length: Varies (1-4 bytes)
 *
 * Bit layout (least significant bits of varint):
 * - Bit 0: I (Independent frame)
 * - Bit 1: D (Discardable frame)
 * - Bit 2: B (Base layer sync point)
 * - Bits 3-5: TID (Temporal Layer ID, 0-7)
 * - Bits 6-7: SID (Spatial Layer ID, 0-3)
 * - Bits 8+: Reserved
 *
 * Reference: draft-ietf-moq-loc Section 2.3.2.2
 *            RFC9626
 */
public class VideoFrameMarkingExtension extends LocHeaderExtension {

    public static final int EXTENSION_ID = 4;

    private boolean independent;
    private boolean discardable;
    private boolean baseLayerSync;
    private int temporalLayerId; // 0-7
    private int spatialLayerId;  // 0-3

    public VideoFrameMarkingExtension() {
        super(EXTENSION_ID);
    }

    public VideoFrameMarkingExtension(boolean independent, boolean discardable,
                                     boolean baseLayerSync, int temporalLayerId, int spatialLayerId) {
        super(EXTENSION_ID);
        this.independent = independent;
        this.discardable = discardable;
        this.baseLayerSync = baseLayerSync;
        setTemporalLayerId(temporalLayerId);
        setSpatialLayerId(spatialLayerId);
    }

    public boolean isIndependent() {
        return independent;
    }

    public void setIndependent(boolean independent) {
        this.independent = independent;
    }

    public boolean isDiscardable() {
        return discardable;
    }

    public void setDiscardable(boolean discardable) {
        this.discardable = discardable;
    }

    public boolean isBaseLayerSync() {
        return baseLayerSync;
    }

    public void setBaseLayerSync(boolean baseLayerSync) {
        this.baseLayerSync = baseLayerSync;
    }

    public int getTemporalLayerId() {
        return temporalLayerId;
    }

    public void setTemporalLayerId(int temporalLayerId) {
        if (temporalLayerId < 0 || temporalLayerId > 7) {
            throw new IllegalArgumentException("Temporal Layer ID must be 0-7");
        }
        this.temporalLayerId = temporalLayerId;
    }

    public int getSpatialLayerId() {
        return spatialLayerId;
    }

    public void setSpatialLayerId(int spatialLayerId) {
        if (spatialLayerId < 0 || spatialLayerId > 3) {
            throw new IllegalArgumentException("Spatial Layer ID must be 0-3");
        }
        this.spatialLayerId = spatialLayerId;
    }

    @Override
    protected byte[] serializeValue() throws IOException {
        long value = 0;

        if (independent) value |= 0x01;
        if (discardable) value |= 0x02;
        if (baseLayerSync) value |= 0x04;
        value |= (temporalLayerId & 0x07) << 3;
        value |= (spatialLayerId & 0x03) << 6;

        return serializeVarint(value);
    }

    @Override
    public void deserializeValue(ByteBuffer buffer, int length) throws IOException {
        long value = readVarint(buffer);

        this.independent = (value & 0x01) != 0;
        this.discardable = (value & 0x02) != 0;
        this.baseLayerSync = (value & 0x04) != 0;
        this.temporalLayerId = (int) ((value >> 3) & 0x07);
        this.spatialLayerId = (int) ((value >> 6) & 0x03);
    }

    @Override
    public String toString() {
        return "VideoFrameMarkingExtension{" +
                "independent=" + independent +
                ", discardable=" + discardable +
                ", baseLayerSync=" + baseLayerSync +
                ", temporalLayerId=" + temporalLayerId +
                ", spatialLayerId=" + spatialLayerId +
                '}';
    }
}
