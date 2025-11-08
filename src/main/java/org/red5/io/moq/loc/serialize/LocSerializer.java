package org.red5.io.moq.loc.serialize;

import org.red5.io.moq.loc.model.LocObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Serializes LOC (Low Overhead Media Container) objects to byte arrays.
 *
 * The serialized format follows draft-ietf-moq-loc:
 * - LOC Header Extensions (optional metadata)
 * - LOC Payload (encoded media data)
 *
 * Reference: draft-ietf-moq-loc (draft-mzanaty-moq-loc-05)
 */
public class LocSerializer {

    private static final Logger logger = LoggerFactory.getLogger(LocSerializer.class);

    /**
     * Serialize a LOC object to a byte array containing header extensions and payload.
     *
     * @param locObject the LOC object to serialize
     * @return byte array with serialized data
     * @throws IOException if serialization fails
     */
    public byte[] serialize(LocObject locObject) throws IOException {
        if (locObject == null) {
            throw new IllegalArgumentException("LocObject cannot be null");
        }

        logger.debug("Serializing LOC object: groupId={}, objectId={}, mediaType={}",
                locObject.getGroupId(), locObject.getObjectId(), locObject.getMediaType());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Serialize header extensions
        byte[] headerExtensions = locObject.serializeHeaderExtensions();
        if (headerExtensions.length > 0) {
            logger.debug("Serializing {} header extension bytes", headerExtensions.length);
            baos.write(headerExtensions);
        }

        // Serialize payload
        byte[] payload = locObject.getPayload();
        if (payload != null && payload.length > 0) {
            logger.debug("Serializing {} payload bytes", payload.length);
            baos.write(payload);
        }

        byte[] result = baos.toByteArray();
        logger.debug("Serialized LOC object: {} total bytes", result.length);

        return result;
    }

    /**
     * Serialize only the header extensions portion.
     *
     * @param locObject the LOC object
     * @return byte array with serialized header extensions
     * @throws IOException if serialization fails
     */
    public byte[] serializeHeaderExtensions(LocObject locObject) throws IOException {
        if (locObject == null) {
            throw new IllegalArgumentException("LocObject cannot be null");
        }

        return locObject.serializeHeaderExtensions();
    }

    /**
     * Get the payload portion of a LOC object.
     *
     * @param locObject the LOC object
     * @return payload byte array
     */
    public byte[] getPayload(LocObject locObject) {
        if (locObject == null) {
            throw new IllegalArgumentException("LocObject cannot be null");
        }

        return locObject.getPayload();
    }

    /**
     * Create a minimal audio LOC object for testing.
     *
     * @param payload the audio data
     * @param timestampMicros capture timestamp in microseconds
     * @return a new LOC object
     */
    public static LocObject createMinimalAudioObject(byte[] payload, long timestampMicros) {
        LocObject obj = new LocObject(LocObject.MediaType.AUDIO, payload);
        obj.setCaptureTimestamp(timestampMicros);
        return obj;
    }

    /**
     * Create a minimal video LOC object for testing.
     *
     * @param payload the video data
     * @param timestampMicros capture timestamp in microseconds
     * @param isKeyFrame whether this is an independent/key frame
     * @return a new LOC object
     */
    public static LocObject createMinimalVideoObject(byte[] payload, long timestampMicros, boolean isKeyFrame) {
        LocObject obj = new LocObject(LocObject.MediaType.VIDEO, payload);
        obj.setCaptureTimestamp(timestampMicros);
        obj.setVideoFrameMarking(isKeyFrame, false, isKeyFrame, 0, 0);
        return obj;
    }
}
