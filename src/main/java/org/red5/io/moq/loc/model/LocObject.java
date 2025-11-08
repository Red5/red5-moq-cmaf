package org.red5.io.moq.loc.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a LOC (Low Overhead Media Container) Object for MoQ Transport.
 *
 * A LOC Object consists of:
 * - LOC Header Extensions (optional metadata)
 * - LOC Payload (encoded audio/video data)
 *
 * The LOC Payload is the "internal data" of an EncodedAudioChunk or EncodedVideoChunk
 * from WebCodecs, which is the elementary bitstream format without any encapsulation.
 *
 * LOC Objects are designed for low-overhead media streaming over MOQT (Media over QUIC Transport)
 * and are used in the WARP streaming format.
 *
 * Reference: draft-ietf-moq-loc (draft-mzanaty-moq-loc-05)
 * https://datatracker.ietf.org/doc/html/draft-mzanaty-moq-loc-05
 */
public class LocObject {

    /**
     * Media type for the LOC object.
     */
    public enum MediaType {
        AUDIO,
        VIDEO
    }

    private MediaType mediaType;
    private byte[] payload;
    private List<LocHeaderExtension> headerExtensions;

    // MOQ Transport identifiers
    private long groupId;
    private long objectId;
    private long subgroupId;

    public LocObject() {
        this.headerExtensions = new ArrayList<>();
    }

    public LocObject(MediaType mediaType, byte[] payload) {
        this();
        this.mediaType = mediaType;
        this.payload = payload;
    }

    // Getters and Setters

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public List<LocHeaderExtension> getHeaderExtensions() {
        return headerExtensions;
    }

    public void setHeaderExtensions(List<LocHeaderExtension> headerExtensions) {
        this.headerExtensions = headerExtensions;
    }

    public void addHeaderExtension(LocHeaderExtension extension) {
        this.headerExtensions.add(extension);
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    public long getSubgroupId() {
        return subgroupId;
    }

    public void setSubgroupId(long subgroupId) {
        this.subgroupId = subgroupId;
    }

    // Helper methods for specific extensions

    /**
     * Get the capture timestamp extension if present.
     */
    public CaptureTimestampExtension getCaptureTimestamp() {
        return getExtension(CaptureTimestampExtension.class);
    }

    /**
     * Set the capture timestamp.
     */
    public void setCaptureTimestamp(long timestampMicros) {
        removeExtension(CaptureTimestampExtension.class);
        addHeaderExtension(new CaptureTimestampExtension(timestampMicros));
    }

    /**
     * Get the video frame marking extension if present.
     */
    public VideoFrameMarkingExtension getVideoFrameMarking() {
        return getExtension(VideoFrameMarkingExtension.class);
    }

    /**
     * Set video frame marking.
     */
    public void setVideoFrameMarking(boolean independent, boolean discardable,
                                    boolean baseLayerSync, int temporalLayerId, int spatialLayerId) {
        removeExtension(VideoFrameMarkingExtension.class);
        addHeaderExtension(new VideoFrameMarkingExtension(independent, discardable,
                baseLayerSync, temporalLayerId, spatialLayerId));
    }

    /**
     * Get the audio level extension if present.
     */
    public AudioLevelExtension getAudioLevel() {
        return getExtension(AudioLevelExtension.class);
    }

    /**
     * Set audio level.
     */
    public void setAudioLevel(boolean voiceActivity, int audioLevel) {
        removeExtension(AudioLevelExtension.class);
        addHeaderExtension(new AudioLevelExtension(voiceActivity, audioLevel));
    }

    /**
     * Get the video config extension if present.
     */
    public VideoConfigExtension getVideoConfig() {
        return getExtension(VideoConfigExtension.class);
    }

    /**
     * Set video config (codec extradata).
     */
    public void setVideoConfig(byte[] configData) {
        removeExtension(VideoConfigExtension.class);
        addHeaderExtension(new VideoConfigExtension(configData));
    }

    /**
     * Get an extension by class type.
     */
    @SuppressWarnings("unchecked")
    private <T extends LocHeaderExtension> T getExtension(Class<T> extensionClass) {
        for (LocHeaderExtension ext : headerExtensions) {
            if (extensionClass.isInstance(ext)) {
                return (T) ext;
            }
        }
        return null;
    }

    /**
     * Remove an extension by class type.
     */
    private void removeExtension(Class<? extends LocHeaderExtension> extensionClass) {
        headerExtensions.removeIf(extensionClass::isInstance);
    }

    /**
     * Serialize the header extensions to bytes.
     */
    public byte[] serializeHeaderExtensions() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (LocHeaderExtension extension : headerExtensions) {
            baos.write(extension.serialize());
        }

        return baos.toByteArray();
    }

    /**
     * Get the total size of the object (headers + payload).
     */
    public int getTotalSize() throws IOException {
        return serializeHeaderExtensions().length + (payload != null ? payload.length : 0);
    }

    /**
     * Check if this is an independent frame (for video).
     */
    public boolean isIndependentFrame() {
        VideoFrameMarkingExtension marking = getVideoFrameMarking();
        return marking != null && marking.isIndependent();
    }

    /**
     * Check if this frame is discardable (for video).
     */
    public boolean isDiscardableFrame() {
        VideoFrameMarkingExtension marking = getVideoFrameMarking();
        return marking != null && marking.isDiscardable();
    }

    @Override
    public String toString() {
        return "LocObject{" +
                "mediaType=" + mediaType +
                ", payloadSize=" + (payload != null ? payload.length : 0) +
                ", headerExtensions=" + headerExtensions.size() +
                ", groupId=" + groupId +
                ", objectId=" + objectId +
                ", subgroupId=" + subgroupId +
                '}';
    }
}
