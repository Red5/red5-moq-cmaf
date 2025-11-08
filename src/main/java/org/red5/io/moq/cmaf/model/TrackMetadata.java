package org.red5.io.moq.cmaf.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Track metadata containing codec configuration and media parameters.
 * This is a convenience class that encapsulates metadata typically found in
 * the initialization segment (moov box), particularly useful for MoQ use cases
 * where initialization data may need to be transmitted separately or inline.
 */
public class TrackMetadata {

    /**
     * Track Header Box (tkhd) - contains visual dimensions and flags.
     * ISO/IEC 14496-12 Section 8.3.2
     */
    public static class TkhdBox extends Box {
        private int version;
        private int flags;
        private long creationTime;
        private long modificationTime;
        private long trackId;
        private long duration;
        private int layer;
        private int alternateGroup;
        private int volume; // 16-bit fixed point (8.8)
        private int[] matrix = new int[9]; // transformation matrix
        private int width;  // 16.16 fixed point
        private int height; // 16.16 fixed point

        public TkhdBox() {
            super("tkhd");
            // Default identity matrix
            matrix[0] = 0x00010000; // a
            matrix[4] = 0x00010000; // e
            matrix[8] = 0x40000000; // i
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public int getFlags() {
            return flags;
        }

        public void setFlags(int flags) {
            this.flags = flags;
        }

        public long getTrackId() {
            return trackId;
        }

        public void setTrackId(long trackId) {
            this.trackId = trackId;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public int getVolume() {
            return volume;
        }

        public void setVolume(int volume) {
            this.volume = volume;
        }

        /**
         * Get width in pixels.
         */
        public int getWidthPixels() {
            return width >> 16;
        }

        /**
         * Set width in pixels.
         */
        public void setWidthPixels(int widthPixels) {
            this.width = widthPixels << 16;
        }

        /**
         * Get height in pixels.
         */
        public int getHeightPixels() {
            return height >> 16;
        }

        /**
         * Set height in pixels.
         */
        public void setHeightPixels(int heightPixels) {
            this.height = heightPixels << 16;
        }

        @Override
        protected long calculateSize() {
            return version == 1 ? 104 : 92;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);

            int versionFlags = (version << 24) | (flags & 0xFFFFFF);
            buffer.putInt(versionFlags);

            if (version == 1) {
                buffer.putLong(creationTime);
                buffer.putLong(modificationTime);
                buffer.putInt((int) trackId);
                buffer.putInt(0); // reserved
                buffer.putLong(duration);
            } else {
                buffer.putInt((int) creationTime);
                buffer.putInt((int) modificationTime);
                buffer.putInt((int) trackId);
                buffer.putInt(0); // reserved
                buffer.putInt((int) duration);
            }

            buffer.putInt(0); // reserved
            buffer.putInt(0); // reserved
            buffer.putShort((short) layer);
            buffer.putShort((short) alternateGroup);
            buffer.putShort((short) volume);
            buffer.putShort((short) 0); // reserved

            for (int m : matrix) {
                buffer.putInt(m);
            }

            buffer.putInt(width);
            buffer.putInt(height);

            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);

            int versionFlags = buffer.getInt();
            this.version = (versionFlags >> 24) & 0xFF;
            this.flags = versionFlags & 0xFFFFFF;

            if (version == 1) {
                this.creationTime = buffer.getLong();
                this.modificationTime = buffer.getLong();
                this.trackId = Integer.toUnsignedLong(buffer.getInt());
                buffer.getInt(); // reserved
                this.duration = buffer.getLong();
            } else {
                this.creationTime = Integer.toUnsignedLong(buffer.getInt());
                this.modificationTime = Integer.toUnsignedLong(buffer.getInt());
                this.trackId = Integer.toUnsignedLong(buffer.getInt());
                buffer.getInt(); // reserved
                this.duration = Integer.toUnsignedLong(buffer.getInt());
            }

            buffer.getInt(); // reserved
            buffer.getInt(); // reserved
            this.layer = buffer.getShort();
            this.alternateGroup = buffer.getShort();
            this.volume = buffer.getShort() & 0xFFFF;
            buffer.getShort(); // reserved

            for (int i = 0; i < 9; i++) {
                matrix[i] = buffer.getInt();
            }

            this.width = buffer.getInt();
            this.height = buffer.getInt();
        }

        @Override
        public String toString() {
            return String.format("TkhdBox{trackId=%d, width=%d, height=%d, duration=%d}",
                    trackId, getWidthPixels(), getHeightPixels(), duration);
        }
    }

    /**
     * Sample Description Box (stsd) - contains sample entries.
     * ISO/IEC 14496-12 Section 8.5.2
     */
    public static class StsdBox extends Box {
        private int version;
        private int flags;
        private int entryCount;
        private SampleEntry[] entries;

        public StsdBox() {
            super("stsd");
        }

        public SampleEntry[] getEntries() {
            return entries;
        }

        public void setEntries(SampleEntry[] entries) {
            this.entries = entries;
            this.entryCount = entries != null ? entries.length : 0;
        }

        public int getEntryCount() {
            return entryCount;
        }

        @Override
        protected long calculateSize() {
            long size = 8 + 4 + 4; // header + version/flags + entry_count
            if (entries != null) {
                for (SampleEntry entry : entries) {
                    size += entry.calculateSize();
                }
            }
            return size;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ByteBuffer header = ByteBuffer.allocate(16);
            writeHeader(header);
            header.putInt((version << 24) | (flags & 0xFFFFFF));
            header.putInt(entryCount);
            baos.write(header.array());

            if (entries != null) {
                for (SampleEntry entry : entries) {
                    baos.write(entry.serialize());
                }
            }

            return baos.toByteArray();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);

            int versionFlags = buffer.getInt();
            this.version = (versionFlags >> 24) & 0xFF;
            this.flags = versionFlags & 0xFFFFFF;
            this.entryCount = buffer.getInt();

            this.entries = new SampleEntry[entryCount];
            for (int i = 0; i < entryCount; i++) {
                // Peek at format to determine entry type
                int pos = buffer.position();
                buffer.getInt(); // size
                byte[] formatBytes = new byte[4];
                buffer.get(formatBytes);
                String format = new String(formatBytes);
                buffer.position(pos);

                if (isVideoCodec(format)) {
                    entries[i] = new VisualSampleEntry();
                } else if (isAudioCodec(format)) {
                    entries[i] = new AudioSampleEntry();
                } else {
                    entries[i] = new SampleEntry();
                }
                entries[i].deserialize(buffer);
            }
        }

        private boolean isVideoCodec(String format) {
            return format.equals("avc1") || format.equals("avc3") ||
                   format.equals("hev1") || format.equals("hvc1") ||
                   format.equals("vp09") || format.equals("av01");
        }

        private boolean isAudioCodec(String format) {
            return format.equals("mp4a") || format.equals("opus") ||
                   format.equals("Opus") || format.equals("ac-3") ||
                   format.equals("ec-3");
        }
    }

    /**
     * Base class for sample entries.
     */
    public static class SampleEntry extends Box {
        protected byte[] reserved = new byte[6];
        protected int dataReferenceIndex;

        public SampleEntry() {
            super("");
        }

        public SampleEntry(String format) {
            super(format);
            this.dataReferenceIndex = 1;
        }

        public int getDataReferenceIndex() {
            return dataReferenceIndex;
        }

        public void setDataReferenceIndex(int dataReferenceIndex) {
            this.dataReferenceIndex = dataReferenceIndex;
        }

        public String getFormat() {
            return getType();
        }

        public void setFormat(String format) {
            setType(format);
        }

        @Override
        protected long calculateSize() {
            return 8 + 8 + 2; // header + reserved + data_reference_index
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);
            buffer.put(reserved);
            buffer.putShort((short) dataReferenceIndex);
            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);
            buffer.get(reserved);
            this.dataReferenceIndex = buffer.getShort() & 0xFFFF;
        }
    }

    /**
     * Visual (video) sample entry.
     * ISO/IEC 14496-12 Section 12.1.3
     */
    public static class VisualSampleEntry extends SampleEntry {
        private int width;
        private int height;
        private int horizresolution = 0x00480000; // 72 dpi
        private int vertresolution = 0x00480000;  // 72 dpi
        private int frameCount = 1;
        private String compressorName = "";
        private int depth = 0x0018; // 24-bit color
        private byte[] codecConfig; // Codec-specific configuration (e.g., avcC, hvcC)

        public VisualSampleEntry() {
            super();
        }

        public VisualSampleEntry(String codecFourCC) {
            super(codecFourCC);
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public String getCompressorName() {
            return compressorName;
        }

        public void setCompressorName(String compressorName) {
            this.compressorName = compressorName;
        }

        public byte[] getCodecConfig() {
            return codecConfig;
        }

        public void setCodecConfig(byte[] codecConfig) {
            this.codecConfig = codecConfig;
        }

        @Override
        protected long calculateSize() {
            // SampleEntry base: 8 (header) + 6 (reserved) + 2 (data_reference_index) = 16
            // VisualSampleEntry fields: 70 bytes
            long size = 8 + 8 + 70; // header + SampleEntry + VisualSampleEntry
            if (codecConfig != null) {
                size += codecConfig.length;
            }
            return size;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Write SampleEntry fields
            ByteBuffer header = ByteBuffer.allocate(16);
            writeHeader(header);
            header.put(reserved);
            header.putShort((short) dataReferenceIndex);
            baos.write(header.array());

            // Write VisualSampleEntry fields
            ByteBuffer visual = ByteBuffer.allocate(70);
            visual.putShort((short) 0); // pre_defined
            visual.putShort((short) 0); // reserved
            visual.putInt(0); // pre_defined[0]
            visual.putInt(0); // pre_defined[1]
            visual.putInt(0); // pre_defined[2]
            visual.putShort((short) width);
            visual.putShort((short) height);
            visual.putInt(horizresolution);
            visual.putInt(vertresolution);
            visual.putInt(0); // reserved
            visual.putShort((short) frameCount);

            // Compressor name (32 bytes, first byte is length)
            byte[] nameBytes = new byte[32];
            if (compressorName != null && !compressorName.isEmpty()) {
                byte[] nameData = compressorName.getBytes(StandardCharsets.UTF_8);
                nameBytes[0] = (byte) Math.min(nameData.length, 31);
                System.arraycopy(nameData, 0, nameBytes, 1, Math.min(nameData.length, 31));
            }
            visual.put(nameBytes);

            visual.putShort((short) depth);
            visual.putShort((short) -1); // pre_defined
            baos.write(visual.array());

            // Write codec configuration if present
            if (codecConfig != null) {
                baos.write(codecConfig);
            }

            return baos.toByteArray();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            long boxSize = readHeader(buffer);
            buffer.get(reserved);
            this.dataReferenceIndex = buffer.getShort() & 0xFFFF;

            buffer.getShort(); // pre_defined
            buffer.getShort(); // reserved
            buffer.getInt(); // pre_defined[0]
            buffer.getInt(); // pre_defined[1]
            buffer.getInt(); // pre_defined[2]
            this.width = buffer.getShort() & 0xFFFF;
            this.height = buffer.getShort() & 0xFFFF;
            this.horizresolution = buffer.getInt();
            this.vertresolution = buffer.getInt();
            buffer.getInt(); // reserved
            this.frameCount = buffer.getShort() & 0xFFFF;

            // Read compressor name
            byte[] nameBytes = new byte[32];
            buffer.get(nameBytes);
            int nameLen = nameBytes[0] & 0xFF;
            if (nameLen > 0 && nameLen <= 31) {
                this.compressorName = new String(nameBytes, 1, nameLen, StandardCharsets.UTF_8);
            }

            this.depth = buffer.getShort() & 0xFFFF;
            buffer.getShort(); // pre_defined

            // Read remaining bytes as codec config
            // Box size includes: 8-byte header + 8-byte SampleEntry base + 70-byte VisualSampleEntry fields
            int totalFieldsSize = 8 + 8 + 70; // 86 bytes total
            int remainingBytes = (int) (boxSize - totalFieldsSize);
            if (remainingBytes > 0 && buffer.remaining() >= remainingBytes) {
                codecConfig = new byte[remainingBytes];
                buffer.get(codecConfig);
            }
        }

        @Override
        public String toString() {
            return String.format("VisualSampleEntry{codec='%s', width=%d, height=%d, depth=%d}",
                    type, width, height, depth);
        }
    }

    /**
     * Audio sample entry.
     * ISO/IEC 14496-12 Section 12.2.3
     */
    public static class AudioSampleEntry extends SampleEntry {
        private int channelCount = 2;
        private int sampleSize = 16;
        private int sampleRate; // 16.16 fixed point
        private byte[] codecConfig; // Codec-specific configuration

        public AudioSampleEntry() {
            super();
        }

        public AudioSampleEntry(String codecFourCC) {
            super(codecFourCC);
        }

        public int getChannelCount() {
            return channelCount;
        }

        public void setChannelCount(int channelCount) {
            this.channelCount = channelCount;
        }

        public int getSampleSize() {
            return sampleSize;
        }

        public void setSampleSize(int sampleSize) {
            this.sampleSize = sampleSize;
        }

        /**
         * Get sample rate in Hz.
         */
        public int getSampleRateHz() {
            return sampleRate >>> 16; // Use unsigned right shift to avoid sign extension
        }

        /**
         * Set sample rate in Hz.
         */
        public void setSampleRateHz(int sampleRateHz) {
            this.sampleRate = sampleRateHz << 16;
        }

        public byte[] getCodecConfig() {
            return codecConfig;
        }

        public void setCodecConfig(byte[] codecConfig) {
            this.codecConfig = codecConfig;
        }

        @Override
        protected long calculateSize() {
            // SampleEntry base: 8 (header) + 6 (reserved) + 2 (data_reference_index) = 16
            // AudioSampleEntry fields: 20 bytes
            long size = 8 + 8 + 20; // header + SampleEntry + AudioSampleEntry
            if (codecConfig != null) {
                size += codecConfig.length;
            }
            return size;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Write SampleEntry fields
            ByteBuffer header = ByteBuffer.allocate(16);
            writeHeader(header);
            header.put(reserved);
            header.putShort((short) dataReferenceIndex);
            baos.write(header.array());

            // Write AudioSampleEntry fields
            ByteBuffer audio = ByteBuffer.allocate(20);
            audio.putInt(0); // reserved[0]
            audio.putInt(0); // reserved[1]
            audio.putShort((short) channelCount);
            audio.putShort((short) sampleSize);
            audio.putShort((short) 0); // pre_defined
            audio.putShort((short) 0); // reserved
            audio.putInt(sampleRate);
            baos.write(audio.array());

            // Write codec configuration if present
            if (codecConfig != null) {
                baos.write(codecConfig);
            }

            return baos.toByteArray();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            long boxSize = readHeader(buffer);
            buffer.get(reserved);
            this.dataReferenceIndex = buffer.getShort() & 0xFFFF;

            buffer.getInt(); // reserved[0]
            buffer.getInt(); // reserved[1]
            this.channelCount = buffer.getShort() & 0xFFFF;
            this.sampleSize = buffer.getShort() & 0xFFFF;
            buffer.getShort(); // pre_defined
            buffer.getShort(); // reserved
            this.sampleRate = buffer.getInt();

            // Read remaining bytes as codec config
            // Box size includes: 8-byte header + 8-byte SampleEntry base + 20-byte AudioSampleEntry fields
            int totalFieldsSize = 8 + 8 + 20; // 36 bytes total
            int remainingBytes = (int) (boxSize - totalFieldsSize);
            if (remainingBytes > 0 && buffer.remaining() >= remainingBytes) {
                codecConfig = new byte[remainingBytes];
                buffer.get(codecConfig);
            }
        }

        @Override
        public String toString() {
            return String.format("AudioSampleEntry{codec='%s', channels=%d, sampleRate=%d Hz, sampleSize=%d bits}",
                    type, channelCount, getSampleRateHz(), sampleSize);
        }
    }
}
