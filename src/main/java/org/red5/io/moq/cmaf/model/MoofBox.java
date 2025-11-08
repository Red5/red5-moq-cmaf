package org.red5.io.moq.cmaf.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Movie Fragment Box (moof) - ISO/IEC 14496-12.
 * Contains metadata about a fragment of media data.
 * Contains mfhd (Movie Fragment Header) and one or more traf (Track Fragment) boxes.
 */
public class MoofBox extends Box {
    private MfhdBox mfhd; // Movie Fragment Header Box
    private List<TrafBox> trafs; // Track Fragment Boxes

    public MoofBox() {
        super("moof");
        this.trafs = new ArrayList<>();
    }

    public MfhdBox getMfhd() {
        return mfhd;
    }

    public void setMfhd(MfhdBox mfhd) {
        this.mfhd = mfhd;
    }

    public List<TrafBox> getTrafs() {
        return trafs;
    }

    public void setTrafs(List<TrafBox> trafs) {
        this.trafs = trafs;
    }

    public void addTraf(TrafBox traf) {
        this.trafs.add(traf);
    }

    @Override
    protected long calculateSize() {
        long size = 8; // header
        if (mfhd != null) {
            size += mfhd.calculateSize();
        }
        for (TrafBox traf : trafs) {
            size += traf.calculateSize();
        }
        return size;
    }

    @Override
    public byte[] serialize() throws IOException {
        this.size = calculateSize();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Write header
        ByteBuffer headerBuffer = ByteBuffer.allocate(8);
        writeHeader(headerBuffer);
        baos.write(headerBuffer.array());

        // Write mfhd
        if (mfhd != null) {
            baos.write(mfhd.serialize());
        }

        // Write trafs
        for (TrafBox traf : trafs) {
            baos.write(traf.serialize());
        }

        return baos.toByteArray();
    }

    @Override
    public void deserialize(ByteBuffer buffer) throws IOException {
        // Read header
        long boxSize = readHeader(buffer);
        int startPosition = buffer.position();
        int endPosition = (int) (startPosition + boxSize - 8);

        // Read child boxes with infinite loop protection
        int loopCount = 0;
        final int MAX_LOOPS = 1000; // Safety limit

        while (buffer.position() < endPosition && loopCount < MAX_LOOPS) {
            loopCount++;

            // Ensure we have at least 8 bytes for child box header
            if (buffer.remaining() < 8) {
                break; // Not enough data
            }

            int positionBefore = buffer.position();

            // Read child box header
            int childSize = buffer.getInt();
            byte[] typeBytes = new byte[4];
            buffer.get(typeBytes);
            String childType = new String(typeBytes);

            // Validate child size
            if (childSize < 8 || childSize > buffer.capacity()) {
                throw new IOException("Invalid child box size: " + childSize);
            }

            // Ensure we have enough data for the full child box
            int childDataSize = childSize - 8; // Subtract header size
            if (buffer.remaining() < childDataSize) {
                buffer.position(positionBefore); // Reset and break
                break;
            }

            // Reset position to read full box (including header)
            buffer.position(positionBefore);

            try {
                if ("mfhd".equals(childType)) {
                    mfhd = new MfhdBox();
                    mfhd.deserialize(buffer);
                } else if ("traf".equals(childType)) {
                    TrafBox traf = new TrafBox();
                    traf.deserialize(buffer);
                    trafs.add(traf);
                } else {
                    // Skip unknown box - move position by childSize
                    buffer.position(positionBefore + childSize);
                }
            } catch (Exception e) {
                // If deserialize fails, skip this box to avoid infinite loop
                buffer.position(positionBefore + childSize);
                throw new IOException("Error deserializing child box '" + childType + "': " + e.getMessage(), e);
            }

            // Safety check: ensure position advanced
            if (buffer.position() <= positionBefore) {
                throw new IOException("Buffer position did not advance for box type: " + childType);
            }
        }

        if (loopCount >= MAX_LOOPS) {
            throw new IOException("Infinite loop detected while deserializing MoofBox");
        }
    }

    @Override
    public String toString() {
        return "MoofBox{" +
                "type='" + type + "', " +
                "size=" + size + ", " +
                "mfhd=" + mfhd + ", " +
                "trafs=" + trafs.size() +
                '}';
    }

    /**
     * Movie Fragment Header Box (mfhd).
     */
    public static class MfhdBox extends Box {
        private long sequenceNumber;

        public MfhdBox() {
            super("mfhd");
        }

        public MfhdBox(long sequenceNumber) {
            super("mfhd");
            this.sequenceNumber = sequenceNumber;
            this.size = calculateSize();
        }

        public long getSequenceNumber() {
            return sequenceNumber;
        }

        public void setSequenceNumber(long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }

        @Override
        protected long calculateSize() {
            return 8 + 4 + 4; // header + version/flags + sequence_number
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);
            buffer.putInt(0); // version (1 byte) + flags (3 bytes)
            buffer.putInt((int) sequenceNumber);
            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);
            buffer.getInt(); // skip version and flags
            this.sequenceNumber = Integer.toUnsignedLong(buffer.getInt());
        }

        @Override
        public String toString() {
            return "MfhdBox{sequenceNumber=" + sequenceNumber + "}";
        }
    }

    /**
     * Track Fragment Box (traf).
     * Contains track fragment information.
     */
    public static class TrafBox extends Box {
        private TfhdBox tfhd; // Track Fragment Header
        private TfdtBox tfdt; // Track Fragment Decode Time
        private List<TrunBox> truns; // Track Fragment Run boxes

        public TrafBox() {
            super("traf");
            this.truns = new ArrayList<>();
        }

        public TfhdBox getTfhd() {
            return tfhd;
        }

        public void setTfhd(TfhdBox tfhd) {
            this.tfhd = tfhd;
        }

        public TfdtBox getTfdt() {
            return tfdt;
        }

        public void setTfdt(TfdtBox tfdt) {
            this.tfdt = tfdt;
        }

        public List<TrunBox> getTruns() {
            return truns;
        }

        public void addTrun(TrunBox trun) {
            this.truns.add(trun);
        }

        @Override
        protected long calculateSize() {
            long size = 8; // header
            if (tfhd != null) size += tfhd.calculateSize();
            if (tfdt != null) size += tfdt.calculateSize();
            for (TrunBox trun : truns) {
                size += trun.calculateSize();
            }
            return size;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ByteBuffer headerBuffer = ByteBuffer.allocate(8);
            writeHeader(headerBuffer);
            baos.write(headerBuffer.array());

            if (tfhd != null) baos.write(tfhd.serialize());
            if (tfdt != null) baos.write(tfdt.serialize());
            for (TrunBox trun : truns) {
                baos.write(trun.serialize());
            }

            return baos.toByteArray();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            long boxSize = readHeader(buffer);
            int startPosition = buffer.position();
            int endPosition = (int) (startPosition + boxSize - 8);

            // Read child boxes with infinite loop protection
            int loopCount = 0;
            final int MAX_LOOPS = 1000; // Safety limit

            while (buffer.position() < endPosition && loopCount < MAX_LOOPS) {
                loopCount++;

                // Ensure we have at least 8 bytes for child box header
                if (buffer.remaining() < 8) {
                    break; // Not enough data
                }

                int positionBefore = buffer.position();

                // Read child box header
                int childSize = buffer.getInt();
                byte[] typeBytes = new byte[4];
                buffer.get(typeBytes);
                String childType = new String(typeBytes);

                // Validate child size
                if (childSize < 8 || childSize > buffer.capacity()) {
                    throw new IOException("Invalid child box size in TrafBox: " + childSize);
                }

                // Ensure we have enough data for the full child box
                int childDataSize = childSize - 8; // Subtract header size
                if (buffer.remaining() < childDataSize) {
                    buffer.position(positionBefore); // Reset and break
                    break;
                }

                // Reset position to read full box (including header)
                buffer.position(positionBefore);

                try {
                    switch (childType) {
                        case "tfhd" -> {
                            tfhd = new TfhdBox();
                            tfhd.deserialize(buffer);
                        }
                        case "tfdt" -> {
                            tfdt = new TfdtBox();
                            tfdt.deserialize(buffer);
                        }
                        case "trun" -> {
                            TrunBox trun = new TrunBox();
                            trun.deserialize(buffer);
                            truns.add(trun);
                        }
                        default -> buffer.position(positionBefore + childSize);
                    }
                } catch (Exception e) {
                    // If deserialize fails, skip this box to avoid infinite loop
                    buffer.position(positionBefore + childSize);
                    throw new IOException("Error deserializing TrafBox child '" + childType + "': " + e.getMessage(), e);
                }

                // Safety check: ensure position advanced
                if (buffer.position() <= positionBefore) {
                    throw new IOException("Buffer position did not advance for TrafBox child: " + childType);
                }
            }

            if (loopCount >= MAX_LOOPS) {
                throw new IOException("Infinite loop detected while deserializing TrafBox");
            }
        }
    }

    /**
     * Track Fragment Header Box (tfhd).
     * ISO/IEC 14496-12 Section 8.8.7
     *
     * Flags:
     * - 0x000001: base-data-offset-present
     * - 0x000002: sample-description-index-present
     * - 0x000008: default-sample-duration-present
     * - 0x000010: default-sample-size-present
     * - 0x000020: default-sample-flags-present
     * - 0x010000: duration-is-empty
     * - 0x020000: default-base-is-moof
     */
    public static class TfhdBox extends Box {
        private static final int BASE_DATA_OFFSET_PRESENT = 0x000001;
        private static final int SAMPLE_DESCRIPTION_INDEX_PRESENT = 0x000002;
        private static final int DEFAULT_SAMPLE_DURATION_PRESENT = 0x000008;
        private static final int DEFAULT_SAMPLE_SIZE_PRESENT = 0x000010;
        private static final int DEFAULT_SAMPLE_FLAGS_PRESENT = 0x000020;
        private static final int DURATION_IS_EMPTY = 0x010000;
        private static final int DEFAULT_BASE_IS_MOOF = 0x020000;

        private int version;
        private int tfhdFlags;
        private long trackId;
        private long baseDataOffset;
        private int sampleDescriptionIndex;
        private long defaultSampleDuration;
        private long defaultSampleSize;
        private SampleFlags defaultSampleFlags;

        public TfhdBox() {
            super("tfhd");
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public int getTfhdFlags() {
            return tfhdFlags;
        }

        public void setTfhdFlags(int tfhdFlags) {
            this.tfhdFlags = tfhdFlags;
        }

        public long getTrackId() {
            return trackId;
        }

        public void setTrackId(long trackId) {
            this.trackId = trackId;
        }

        public long getBaseDataOffset() {
            return baseDataOffset;
        }

        public void setBaseDataOffset(long baseDataOffset) {
            this.baseDataOffset = baseDataOffset;
            this.tfhdFlags |= BASE_DATA_OFFSET_PRESENT;
        }

        public int getSampleDescriptionIndex() {
            return sampleDescriptionIndex;
        }

        public void setSampleDescriptionIndex(int sampleDescriptionIndex) {
            this.sampleDescriptionIndex = sampleDescriptionIndex;
            this.tfhdFlags |= SAMPLE_DESCRIPTION_INDEX_PRESENT;
        }

        public long getDefaultSampleDuration() {
            return defaultSampleDuration;
        }

        public void setDefaultSampleDuration(long defaultSampleDuration) {
            this.defaultSampleDuration = defaultSampleDuration;
            this.tfhdFlags |= DEFAULT_SAMPLE_DURATION_PRESENT;
        }

        public long getDefaultSampleSize() {
            return defaultSampleSize;
        }

        public void setDefaultSampleSize(long defaultSampleSize) {
            this.defaultSampleSize = defaultSampleSize;
            this.tfhdFlags |= DEFAULT_SAMPLE_SIZE_PRESENT;
        }

        public SampleFlags getDefaultSampleFlags() {
            return defaultSampleFlags;
        }

        public void setDefaultSampleFlags(SampleFlags defaultSampleFlags) {
            this.defaultSampleFlags = defaultSampleFlags;
            this.tfhdFlags |= DEFAULT_SAMPLE_FLAGS_PRESENT;
        }

        @Override
        protected long calculateSize() {
            long size = 8 + 4 + 4; // header + version/flags + track_id

            if ((tfhdFlags & BASE_DATA_OFFSET_PRESENT) != 0) {
                size += 8;
            }
            if ((tfhdFlags & SAMPLE_DESCRIPTION_INDEX_PRESENT) != 0) {
                size += 4;
            }
            if ((tfhdFlags & DEFAULT_SAMPLE_DURATION_PRESENT) != 0) {
                size += 4;
            }
            if ((tfhdFlags & DEFAULT_SAMPLE_SIZE_PRESENT) != 0) {
                size += 4;
            }
            if ((tfhdFlags & DEFAULT_SAMPLE_FLAGS_PRESENT) != 0) {
                size += 4;
            }

            return size;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);

            int versionFlags = (version << 24) | (tfhdFlags & 0xFFFFFF);
            buffer.putInt(versionFlags);
            buffer.putInt((int) trackId);

            if ((tfhdFlags & BASE_DATA_OFFSET_PRESENT) != 0) {
                buffer.putLong(baseDataOffset);
            }
            if ((tfhdFlags & SAMPLE_DESCRIPTION_INDEX_PRESENT) != 0) {
                buffer.putInt(sampleDescriptionIndex);
            }
            if ((tfhdFlags & DEFAULT_SAMPLE_DURATION_PRESENT) != 0) {
                buffer.putInt((int) defaultSampleDuration);
            }
            if ((tfhdFlags & DEFAULT_SAMPLE_SIZE_PRESENT) != 0) {
                buffer.putInt((int) defaultSampleSize);
            }
            if ((tfhdFlags & DEFAULT_SAMPLE_FLAGS_PRESENT) != 0) {
                buffer.putInt(defaultSampleFlags.getFlags());
            }

            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);

            int versionFlags = buffer.getInt();
            this.version = (versionFlags >> 24) & 0xFF;
            this.tfhdFlags = versionFlags & 0xFFFFFF;

            this.trackId = Integer.toUnsignedLong(buffer.getInt());

            if ((tfhdFlags & BASE_DATA_OFFSET_PRESENT) != 0) {
                this.baseDataOffset = buffer.getLong();
            }
            if ((tfhdFlags & SAMPLE_DESCRIPTION_INDEX_PRESENT) != 0) {
                this.sampleDescriptionIndex = buffer.getInt();
            }
            if ((tfhdFlags & DEFAULT_SAMPLE_DURATION_PRESENT) != 0) {
                this.defaultSampleDuration = Integer.toUnsignedLong(buffer.getInt());
            }
            if ((tfhdFlags & DEFAULT_SAMPLE_SIZE_PRESENT) != 0) {
                this.defaultSampleSize = Integer.toUnsignedLong(buffer.getInt());
            }
            if ((tfhdFlags & DEFAULT_SAMPLE_FLAGS_PRESENT) != 0) {
                this.defaultSampleFlags = new SampleFlags(buffer.getInt());
            }
        }

        @Override
        public String toString() {
            return String.format("TfhdBox{trackId=%d, flags=0x%06X, defaultDuration=%d, defaultSize=%d}",
                    trackId, tfhdFlags, defaultSampleDuration, defaultSampleSize);
        }
    }

    /**
     * Track Fragment Decode Time Box (tfdt).
     */
    public static class TfdtBox extends Box {
        private long baseMediaDecodeTime;

        public TfdtBox() {
            super("tfdt");
        }

        public TfdtBox(long baseMediaDecodeTime) {
            super("tfdt");
            this.baseMediaDecodeTime = baseMediaDecodeTime;
        }

        public long getBaseMediaDecodeTime() {
            return baseMediaDecodeTime;
        }

        public void setBaseMediaDecodeTime(long baseMediaDecodeTime) {
            this.baseMediaDecodeTime = baseMediaDecodeTime;
        }

        @Override
        protected long calculateSize() {
            return 8 + 4 + 8; // header + version/flags + baseMediaDecodeTime (64-bit)
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);
            buffer.putInt(0x01000000); // version 1, flags 0
            buffer.putLong(baseMediaDecodeTime);
            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);
            int versionFlags = buffer.getInt();
            int version = (versionFlags >> 24) & 0xFF;
            if (version == 1) {
                this.baseMediaDecodeTime = buffer.getLong();
            } else {
                this.baseMediaDecodeTime = Integer.toUnsignedLong(buffer.getInt());
            }
        }
    }

    /**
     * Track Fragment Run Box (trun).
     * Contains information about samples in the track fragment.
     *
     * Flags define which fields are present:
     * - 0x000001: data-offset-present
     * - 0x000004: first-sample-flags-present
     * - 0x000100: sample-duration-present
     * - 0x000200: sample-size-present
     * - 0x000400: sample-flags-present
     * - 0x000800: sample-composition-time-offsets-present
     */
    public static class TrunBox extends Box {
        // Trun box flags
        private static final int DATA_OFFSET_PRESENT = 0x000001;
        private static final int FIRST_SAMPLE_FLAGS_PRESENT = 0x000004;
        private static final int SAMPLE_DURATION_PRESENT = 0x000100;
        private static final int SAMPLE_SIZE_PRESENT = 0x000200;
        private static final int SAMPLE_FLAGS_PRESENT = 0x000400;
        private static final int SAMPLE_COMPOSITION_TIME_OFFSET_PRESENT = 0x000800;

        private int version;
        private int trunFlags;
        private int sampleCount;
        private int dataOffset;
        private SampleFlags firstSampleFlags;
        private List<Sample> samples;

        public TrunBox() {
            super("trun");
            this.samples = new ArrayList<>();
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public int getTrunFlags() {
            return trunFlags;
        }

        public void setTrunFlags(int trunFlags) {
            this.trunFlags = trunFlags;
        }

        public int getSampleCount() {
            return sampleCount;
        }

        public void setSampleCount(int sampleCount) {
            this.sampleCount = sampleCount;
        }

        public int getDataOffset() {
            return dataOffset;
        }

        public void setDataOffset(int dataOffset) {
            this.dataOffset = dataOffset;
        }

        public SampleFlags getFirstSampleFlags() {
            return firstSampleFlags;
        }

        public void setFirstSampleFlags(SampleFlags firstSampleFlags) {
            this.firstSampleFlags = firstSampleFlags;
        }

        public List<Sample> getSamples() {
            return samples;
        }

        public void addSample(Sample sample) {
            this.samples.add(sample);
        }

        public void setSamples(List<Sample> samples) {
            this.samples = samples;
        }

        @Override
        protected long calculateSize() {
            long size = 8 + 4 + 4; // header + version/flags + sample_count

            if ((trunFlags & DATA_OFFSET_PRESENT) != 0) {
                size += 4; // data_offset
            }
            if ((trunFlags & FIRST_SAMPLE_FLAGS_PRESENT) != 0) {
                size += 4; // first_sample_flags
            }

            // Calculate per-sample data size
            for (int i = 0; i < samples.size(); i++) {
                if ((trunFlags & SAMPLE_DURATION_PRESENT) != 0) {
                    size += 4;
                }
                if ((trunFlags & SAMPLE_SIZE_PRESENT) != 0) {
                    size += 4;
                }
                if ((trunFlags & SAMPLE_FLAGS_PRESENT) != 0) {
                    size += 4;
                }
                if ((trunFlags & SAMPLE_COMPOSITION_TIME_OFFSET_PRESENT) != 0) {
                    size += 4;
                }
            }

            return size;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);

            // Write version and flags
            int versionFlags = (version << 24) | (trunFlags & 0xFFFFFF);
            buffer.putInt(versionFlags);

            // Write sample count
            buffer.putInt(sampleCount);

            // Conditionally write optional fields
            if ((trunFlags & DATA_OFFSET_PRESENT) != 0) {
                buffer.putInt(dataOffset);
            }
            if ((trunFlags & FIRST_SAMPLE_FLAGS_PRESENT) != 0) {
                buffer.putInt(firstSampleFlags.getFlags());
            }

            // Write per-sample data
            for (int i = 0; i < samples.size(); i++) {
                Sample sample = samples.get(i);

                if ((trunFlags & SAMPLE_DURATION_PRESENT) != 0) {
                    buffer.putInt((int) sample.getDuration());
                }
                if ((trunFlags & SAMPLE_SIZE_PRESENT) != 0) {
                    buffer.putInt((int) sample.getSize());
                }
                if ((trunFlags & SAMPLE_FLAGS_PRESENT) != 0) {
                    buffer.putInt(sample.getSampleFlags().getFlags());
                }
                if ((trunFlags & SAMPLE_COMPOSITION_TIME_OFFSET_PRESENT) != 0) {
                    if (version == 0) {
                        buffer.putInt((int) sample.getCompositionTimeOffset());
                    } else {
                        buffer.putInt((int) sample.getCompositionTimeOffset()); // signed in version 1
                    }
                }
            }

            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);

            // Read version and flags
            int versionFlags = buffer.getInt();
            this.version = (versionFlags >> 24) & 0xFF;
            this.trunFlags = versionFlags & 0xFFFFFF;

            // Read sample count
            this.sampleCount = buffer.getInt();

            // Conditionally read optional fields
            if ((trunFlags & DATA_OFFSET_PRESENT) != 0) {
                this.dataOffset = buffer.getInt();
            }
            if ((trunFlags & FIRST_SAMPLE_FLAGS_PRESENT) != 0) {
                this.firstSampleFlags = new SampleFlags(buffer.getInt());
            }

            // Read per-sample data
            this.samples = new ArrayList<>(sampleCount);
            for (int i = 0; i < sampleCount; i++) {
                Sample sample = new Sample();

                if ((trunFlags & SAMPLE_DURATION_PRESENT) != 0) {
                    sample.setDuration(Integer.toUnsignedLong(buffer.getInt()));
                }
                if ((trunFlags & SAMPLE_SIZE_PRESENT) != 0) {
                    sample.setSize(Integer.toUnsignedLong(buffer.getInt()));
                }
                if ((trunFlags & SAMPLE_FLAGS_PRESENT) != 0) {
                    sample.setSampleFlags(new SampleFlags(buffer.getInt()));
                } else if (i == 0 && firstSampleFlags != null) {
                    // First sample uses first_sample_flags if sample_flags not present
                    sample.setSampleFlags(firstSampleFlags);
                }
                if ((trunFlags & SAMPLE_COMPOSITION_TIME_OFFSET_PRESENT) != 0) {
                    if (version == 0) {
                        sample.setCompositionTimeOffset(Integer.toUnsignedLong(buffer.getInt()));
                    } else {
                        // Version 1 uses signed composition time offset
                        sample.setCompositionTimeOffset(buffer.getInt());
                    }
                }

                samples.add(sample);
            }
        }

        @Override
        public String toString() {
            return String.format("TrunBox{version=%d, flags=0x%06X, sampleCount=%d, dataOffset=%d, samples=%d}",
                    version, trunFlags, sampleCount, dataOffset, samples.size());
        }

        /**
         * Represents a single sample within a track fragment run.
         */
        public static class Sample {
            private long duration;
            private long size;
            private SampleFlags sampleFlags;
            private long compositionTimeOffset;

            public long getDuration() {
                return duration;
            }

            public void setDuration(long duration) {
                this.duration = duration;
            }

            public long getSize() {
                return size;
            }

            public void setSize(long size) {
                this.size = size;
            }

            public SampleFlags getSampleFlags() {
                return sampleFlags;
            }

            public void setSampleFlags(SampleFlags sampleFlags) {
                this.sampleFlags = sampleFlags;
            }

            public long getCompositionTimeOffset() {
                return compositionTimeOffset;
            }

            public void setCompositionTimeOffset(long compositionTimeOffset) {
                this.compositionTimeOffset = compositionTimeOffset;
            }

            /**
             * @return true if this sample is a sync sample (key frame)
             */
            public boolean isSyncSample() {
                return sampleFlags != null && sampleFlags.isSyncSample();
            }

            /**
             * @return true if this sample is independent (doesn't depend on other samples)
             */
            public boolean isIndependent() {
                return sampleFlags != null && sampleFlags.isIndependent();
            }

            @Override
            public String toString() {
                return String.format("Sample{duration=%d, size=%d, flags=%s, cto=%d}",
                        duration, size, sampleFlags, compositionTimeOffset);
            }
        }
    }
}
