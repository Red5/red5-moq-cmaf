package org.red5.io.moq.cmaf.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Represents a complete CMAF fragment consisting of:
 * - Segment Type Box (styp)
 * - Movie Fragment Box (moof)
 * - Media Data Box (mdat)
 *
 * This maps to a MoQ object payload according to the draft specification.
 */
public class CmafFragment {
    private StypBox styp;
    private MoofBox moof;
    private MdatBox mdat;

    // Metadata for MoQ Transport
    private long groupId;
    private long objectId;
    private MediaType mediaType;

    public enum MediaType {
        AUDIO,
        VIDEO,
        METADATA,
        OTHER
    }

    public CmafFragment() {
    }

    public CmafFragment(StypBox styp, MoofBox moof, MdatBox mdat) {
        this.styp = styp;
        this.moof = moof;
        this.mdat = mdat;
    }

    public StypBox getStyp() {
        return styp;
    }

    public void setStyp(StypBox styp) {
        this.styp = styp;
    }

    public MoofBox getMoof() {
        return moof;
    }

    public void setMoof(MoofBox moof) {
        this.moof = moof;
    }

    public MdatBox getMdat() {
        return mdat;
    }

    public void setMdat(MdatBox mdat) {
        this.mdat = mdat;
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

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * Serialize the complete CMAF fragment to a byte array.
     * This is the payload that would be transmitted over MoQ Transport.
     *
     * @return serialized fragment
     * @throws IOException if serialization fails
     */
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (styp != null) {
            baos.write(styp.serialize());
        }
        if (moof != null) {
            baos.write(moof.serialize());
        }
        if (mdat != null) {
            baos.write(mdat.serialize());
        }

        return baos.toByteArray();
    }

    /**
     * Deserialize a CMAF fragment from a byte array.
     *
     * @param data byte array containing the fragment
     * @throws IOException if deserialization fails
     */
    public void deserialize(byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // Infinite loop protection
        int loopCount = 0;
        final int MAX_LOOPS = 100; // Reasonable limit for top-level boxes

        while (buffer.hasRemaining() && buffer.remaining() >= 8 && loopCount < MAX_LOOPS) {
            loopCount++;

            // Peek at the box type
            int positionBefore = buffer.position();
            int boxSize = buffer.getInt();
            byte[] typeBytes = new byte[4];
            buffer.get(typeBytes);
            String boxType = new String(typeBytes);

            // Validate box size
            if (boxSize < 8 || boxSize > buffer.capacity()) {
                throw new IOException("Invalid box size in CmafFragment: " + boxSize + " for type: " + boxType);
            }

            // Ensure we have enough data for the full box
            int boxDataSize = boxSize - 8; // Subtract header size
            if (buffer.remaining() < boxDataSize) {
                buffer.position(positionBefore); // Reset
                break; // Not enough data for complete box
            }

            // Reset to read full box (including header)
            buffer.position(positionBefore);

            try {
                switch (boxType) {
                    case "styp" -> {
                        styp = new StypBox();
                        styp.deserialize(buffer);
                    }
                    case "moof" -> {
                        moof = new MoofBox();
                        moof.deserialize(buffer);
                    }
                    case "mdat" -> {
                        mdat = new MdatBox();
                        mdat.deserialize(buffer);
                    }
                    default -> {
                        // Skip unknown box - move position by boxSize
                        buffer.position(positionBefore + boxSize);
                    }
                }
            } catch (Exception e) {
                // If deserialize fails, skip this box to avoid infinite loop
                buffer.position(positionBefore + boxSize);
                throw new IOException("Error deserializing box '" + boxType + "': " + e.getMessage(), e);
            }

            // Safety check: ensure position advanced
            if (buffer.position() <= positionBefore) {
                throw new IOException("Buffer position did not advance for box type: " + boxType);
            }
        }

        if (loopCount >= MAX_LOOPS) {
            throw new IOException("Infinite loop detected while deserializing CmafFragment");
        }
    }

    /**
     * Calculate the total size of the fragment.
     *
     * @return total size in bytes
     */
    public long getTotalSize() {
        long size = 0;
        if (styp != null) size += styp.getSize();
        if (moof != null) size += moof.getSize();
        if (mdat != null) size += mdat.getSize();
        return size;
    }

    /**
     * Get the base media decode time from the fragment.
     *
     * @return decode time or -1 if not available
     */
    public long getBaseMediaDecodeTime() {
        if (moof != null && !moof.getTrafs().isEmpty()) {
            MoofBox.TrafBox traf = moof.getTrafs().get(0);
            if (traf.getTfdt() != null) {
                return traf.getTfdt().getBaseMediaDecodeTime();
            }
        }
        return -1;
    }

    /**
     * Get the sequence number from the fragment.
     *
     * @return sequence number or -1 if not available
     */
    public long getSequenceNumber() {
        if (moof != null && moof.getMfhd() != null) {
            return moof.getMfhd().getSequenceNumber();
        }
        return -1;
    }

    @Override
    public String toString() {
        return "CmafFragment{" +
                "styp=" + styp + ", " +
                "moof=" + moof + ", " +
                "mdat=" + mdat + ", " +
                "groupId=" + groupId + ", " +
                "objectId=" + objectId + ", " +
                "mediaType=" + mediaType + ", " +
                "totalSize=" + getTotalSize() +
                '}';
    }
}
