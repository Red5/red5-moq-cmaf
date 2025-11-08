package org.red5.io.moq.loc.deserialize;

import org.red5.io.moq.loc.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Deserializes LOC (Low Overhead Media Container) objects from byte arrays.
 *
 * The input format follows draft-ietf-moq-loc:
 * - LOC Header Extensions (optional metadata)
 * - LOC Payload (encoded media data)
 *
 * Reference: draft-ietf-moq-loc (draft-mzanaty-moq-loc-05)
 */
public class LocDeserializer {

    private static final Logger logger = LoggerFactory.getLogger(LocDeserializer.class);

    /**
     * Deserialize a LOC object from a byte array.
     *
     * Note: This assumes we know where header extensions end and payload begins.
     * In practice, the MOQ transport layer would provide separate buffers for
     * header extensions and payload.
     *
     * @param headerExtensionBytes the header extension bytes
     * @param payloadBytes the payload bytes
     * @param mediaType the media type
     * @return deserialized LOC object
     * @throws IOException if deserialization fails
     */
    public LocObject deserialize(byte[] headerExtensionBytes, byte[] payloadBytes,
                                  LocObject.MediaType mediaType) throws IOException {
        logger.debug("Deserializing LOC object: headerExtensionBytes={}, payloadBytes={}, mediaType={}",
                headerExtensionBytes != null ? headerExtensionBytes.length : 0,
                payloadBytes != null ? payloadBytes.length : 0,
                mediaType);

        LocObject obj = new LocObject();
        obj.setMediaType(mediaType);

        // Deserialize header extensions
        if (headerExtensionBytes != null && headerExtensionBytes.length > 0) {
            deserializeHeaderExtensions(headerExtensionBytes, obj);
        }

        // Set payload
        obj.setPayload(payloadBytes);

        logger.debug("Deserialized LOC object with {} header extensions",
                obj.getHeaderExtensions().size());

        return obj;
    }

    /**
     * Deserialize header extensions from a byte array.
     *
     * @param data the header extension bytes
     * @param obj the LOC object to populate
     * @throws IOException if deserialization fails
     */
    private void deserializeHeaderExtensions(byte[] data, LocObject obj) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        while (buffer.hasRemaining()) {
            // Read extension ID
            long extensionId = LocHeaderExtension.readVarint(buffer);

            logger.debug("Reading extension ID: {}", extensionId);

            // Determine if this is a varint value (even ID) or byte array (odd ID)
            boolean isVarintValue = (extensionId % 2) == 0;

            int length = 0;
            if (!isVarintValue) {
                // Odd ID: read length
                length = (int) LocHeaderExtension.readVarint(buffer);
                logger.debug("Extension length: {}", length);
            }

            // Create and deserialize the appropriate extension
            LocHeaderExtension extension = createExtension((int) extensionId);
            if (extension != null) {
                extension.deserializeValue(buffer, length);
                obj.addHeaderExtension(extension);
                logger.debug("Deserialized extension: {}", extension);
            } else {
                // Unknown extension, skip it
                logger.warn("Unknown extension ID: {}, skipping", extensionId);
                if (!isVarintValue) {
                    // Skip the value bytes
                    buffer.position(buffer.position() + length);
                } else {
                    // Skip the varint value
                    LocHeaderExtension.readVarint(buffer);
                }
            }
        }
    }

    /**
     * Create an extension instance based on the extension ID.
     *
     * @param extensionId the extension ID
     * @return new extension instance, or null if unknown
     */
    private LocHeaderExtension createExtension(int extensionId) {
        return switch (extensionId) {
            case CaptureTimestampExtension.EXTENSION_ID -> new CaptureTimestampExtension();
            case VideoFrameMarkingExtension.EXTENSION_ID -> new VideoFrameMarkingExtension();
            case AudioLevelExtension.EXTENSION_ID -> new AudioLevelExtension();
            case VideoConfigExtension.EXTENSION_ID -> new VideoConfigExtension();
            default -> null;
        };
    }
}
