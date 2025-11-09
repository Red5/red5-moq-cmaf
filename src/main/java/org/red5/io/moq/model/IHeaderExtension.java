package org.red5.io.moq.model;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Common interface for MoQ header extensions (MOQMI and LOC).
 *
 * This interface defines the contract for both MoqMIHeaderExtension and LocHeaderExtension,
 * allowing them to be stored and processed uniformly in the relay pipeline.
 *
 * Extension ID conventions (common to both MOQMI and LOC):
 * - Even IDs: Value is varint, Length is omitted
 * - Odd IDs: Value is byte array, Length is varint
 *
 * References:
 * - draft-cenzano-moq-media-interop-03 (MOQMI)
 * - draft-ietf-moq-loc (LOC)
 */
public interface IHeaderExtension {

    /**
     * Get the extension ID.
     *
     * @return extension ID as integer
     */
    int getExtensionId();

    /**
     * Check if this extension uses varint encoding for the value.
     * Even IDs use varint, odd IDs use byte array.
     *
     * @return true if value is varint-encoded
     */
    boolean isVarintValue();

    /**
     * Serialize the extension to bytes.
     * Format: [ID][Length (if odd ID)][Value]
     *
     * @return serialized extension bytes
     * @throws IOException if serialization fails
     */
    byte[] serialize() throws IOException;

    /**
     * Deserialize the extension value from a buffer.
     * The extension ID and length (if applicable) have already been read.
     *
     * @param buffer ByteBuffer positioned at the value start
     * @param length value length in bytes (0 if varint-encoded)
     * @throws IOException if deserialization fails
     */
    void deserializeValue(ByteBuffer buffer, int length) throws IOException;

}
