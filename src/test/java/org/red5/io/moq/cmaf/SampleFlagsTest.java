package org.red5.io.moq.cmaf;

import org.red5.io.moq.cmaf.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SampleFlags parsing and interpretation.
 * Tests the parsing of sample flags according to ISO/IEC 14496-12 Section 8.8.3.1.
 */
class SampleFlagsTest {

    @Test
    @DisplayName("Test sync sample (key frame) flags parsing")
    void testSyncSampleFlags() {
        // Sync sample: sampleDependsOn=2 (does not depend), sampleIsNonSync=0 (is sync)
        int flags = (2 << 24); // 0x02000000
        SampleFlags sampleFlags = new SampleFlags(flags);

        assertEquals(2, sampleFlags.getSampleDependsOn());
        assertTrue(sampleFlags.isSyncSample());
        assertFalse(sampleFlags.isSampleNonSync());
        assertTrue(sampleFlags.isIndependent());
    }

    @Test
    @DisplayName("Test non-sync sample (non-key frame) flags parsing")
    void testNonSyncSampleFlags() {
        // Non-sync sample: sampleDependsOn=1 (depends on others), sampleIsNonSync=1
        int flags = (1 << 24) | (1 << 16); // 0x01010000
        SampleFlags sampleFlags = new SampleFlags(flags);

        assertEquals(1, sampleFlags.getSampleDependsOn());
        assertFalse(sampleFlags.isSyncSample());
        assertTrue(sampleFlags.isSampleNonSync());
        assertFalse(sampleFlags.isIndependent());
    }

    @Test
    @DisplayName("Test all sample dependency flags")
    void testSampleDependencyFlags() {
        // Test is_leading (bits 4-5 from MSB, which is bits 26-27 from right)
        SampleFlags flags1 = new SampleFlags(3 << 26);
        assertEquals(3, flags1.getIsLeading());

        // Test sample_depends_on (bits 6-7 from MSB, which is bits 24-25 from right)
        SampleFlags flags2 = new SampleFlags(3 << 24);
        assertEquals(3, flags2.getSampleDependsOn());

        // Test sample_is_depended_on (bits 8-9 from MSB, which is bits 22-23 from right)
        SampleFlags flags3 = new SampleFlags(1 << 22); // Value 1 = others depend on this
        assertEquals(1, flags3.getSampleIsDependedOn());
        assertTrue(flags3.isDependedUpon()); // sampleIsDependedOn == 1

        // Test sample_has_redundancy (bits 10-11 from MSB, which is bits 20-21 from right)
        SampleFlags flags4 = new SampleFlags(3 << 20);
        assertEquals(3, flags4.getSampleHasRedundancy());
    }

    @Test
    @DisplayName("Test sample padding value")
    void testSamplePaddingValue() {
        // Sample padding value is bits 12-14 (3 bits, max value 7)
        int flags = (7 << 17); // Set padding value to 7
        SampleFlags sampleFlags = new SampleFlags(flags);

        assertEquals(7, sampleFlags.getSamplePaddingValue());
    }

    @Test
    @DisplayName("Test sample degradation priority")
    void testSampleDegradationPriority() {
        // Degradation priority is bits 16-31 (16 bits)
        int flags = 0xFFFF; // Max priority
        SampleFlags sampleFlags = new SampleFlags(flags);

        assertEquals(0xFFFF, sampleFlags.getSampleDegradationPriority());
    }

    @Test
    @DisplayName("Test complex sample flags with all fields set")
    void testComplexSampleFlags() {
        // Create flags with multiple fields set:
        // isLeading=2, sampleDependsOn=1, sampleIsDependedOn=1, sampleHasRedundancy=2,
        // paddingValue=3, isNonSync=1, degradationPriority=100
        int flags = (2 << 26) |  // isLeading = 2
                    (1 << 24) |  // sampleDependsOn = 1
                    (1 << 22) |  // sampleIsDependedOn = 1
                    (2 << 20) |  // sampleHasRedundancy = 2
                    (3 << 17) |  // paddingValue = 3
                    (1 << 16) |  // isNonSync = 1
                    100;         // degradationPriority = 100

        SampleFlags sampleFlags = new SampleFlags(flags);

        assertEquals(2, sampleFlags.getIsLeading());
        assertEquals(1, sampleFlags.getSampleDependsOn());
        assertEquals(1, sampleFlags.getSampleIsDependedOn());
        assertEquals(2, sampleFlags.getSampleHasRedundancy());
        assertEquals(3, sampleFlags.getSamplePaddingValue());
        assertTrue(sampleFlags.isSampleNonSync());
        assertEquals(100, sampleFlags.getSampleDegradationPriority());
    }

    @Test
    @DisplayName("Test createSyncSampleFlags factory method")
    void testCreateSyncSampleFlags() {
        SampleFlags flags = SampleFlags.createSyncSampleFlags();

        assertTrue(flags.isSyncSample());
        assertFalse(flags.isSampleNonSync());
        assertTrue(flags.isIndependent());
        assertEquals(2, flags.getSampleDependsOn());
    }

    @Test
    @DisplayName("Test createNonSyncSampleFlags factory method")
    void testCreateNonSyncSampleFlags() {
        SampleFlags flags = SampleFlags.createNonSyncSampleFlags();

        assertFalse(flags.isSyncSample());
        assertTrue(flags.isSampleNonSync());
        assertFalse(flags.isIndependent());
        assertEquals(1, flags.getSampleDependsOn());
    }

    @Test
    @DisplayName("Test TrunBox with sample flags")
    void testTrunBoxWithSampleFlags() throws IOException {
        // Create a trun box with sample flags
        MoofBox.TrunBox trun = new MoofBox.TrunBox();
        trun.setVersion(0);
        trun.setTrunFlags(0x000401); // data_offset_present + sample_flags_present
        trun.setSampleCount(2);
        trun.setDataOffset(100);

        // Create samples with different flags
        MoofBox.TrunBox.Sample sample1 = new MoofBox.TrunBox.Sample();
        sample1.setSampleFlags(SampleFlags.createSyncSampleFlags());

        MoofBox.TrunBox.Sample sample2 = new MoofBox.TrunBox.Sample();
        sample2.setSampleFlags(SampleFlags.createNonSyncSampleFlags());

        trun.addSample(sample1);
        trun.addSample(sample2);

        // Serialize
        byte[] data = trun.serialize();
        assertNotNull(data);
        assertTrue(data.length > 0);

        // Deserialize
        MoofBox.TrunBox deserialized = new MoofBox.TrunBox();
        deserialized.deserialize(ByteBuffer.wrap(data));

        // Verify
        assertEquals(2, deserialized.getSampleCount());
        assertEquals(2, deserialized.getSamples().size());

        // Verify first sample (sync)
        assertTrue(deserialized.getSamples().get(0).isSyncSample());
        assertTrue(deserialized.getSamples().get(0).isIndependent());

        // Verify second sample (non-sync)
        assertFalse(deserialized.getSamples().get(1).isSyncSample());
        assertFalse(deserialized.getSamples().get(1).isIndependent());
    }

    @Test
    @DisplayName("Test TrunBox with all sample data fields")
    void testTrunBoxWithAllSampleFields() throws IOException {
        // Create a trun box with all flags set
        MoofBox.TrunBox trun = new MoofBox.TrunBox();
        trun.setVersion(0);
        trun.setTrunFlags(0x000F01); // data_offset + duration + size + flags + composition_time_offset
        trun.setSampleCount(1);
        trun.setDataOffset(200);

        // Create a sample with all fields
        MoofBox.TrunBox.Sample sample = new MoofBox.TrunBox.Sample();
        sample.setDuration(1000);
        sample.setSize(5000);
        sample.setSampleFlags(SampleFlags.createSyncSampleFlags());
        sample.setCompositionTimeOffset(500);

        trun.addSample(sample);

        // Serialize
        byte[] data = trun.serialize();
        assertNotNull(data);

        // Deserialize
        MoofBox.TrunBox deserialized = new MoofBox.TrunBox();
        deserialized.deserialize(ByteBuffer.wrap(data));

        // Verify
        assertEquals(1, deserialized.getSampleCount());
        assertEquals(1, deserialized.getSamples().size());

        MoofBox.TrunBox.Sample deserializedSample = deserialized.getSamples().get(0);
        assertEquals(1000, deserializedSample.getDuration());
        assertEquals(5000, deserializedSample.getSize());
        assertTrue(deserializedSample.isSyncSample());
        assertEquals(500, deserializedSample.getCompositionTimeOffset());
    }

    @Test
    @DisplayName("Test TrunBox with first_sample_flags")
    void testTrunBoxWithFirstSampleFlags() throws IOException {
        // Create a trun box with first_sample_flags
        MoofBox.TrunBox trun = new MoofBox.TrunBox();
        trun.setVersion(0);
        trun.setTrunFlags(0x000005); // data_offset_present + first_sample_flags_present
        trun.setSampleCount(2);
        trun.setDataOffset(150);
        trun.setFirstSampleFlags(SampleFlags.createSyncSampleFlags());

        // Create samples - first one should inherit first_sample_flags
        MoofBox.TrunBox.Sample sample1 = new MoofBox.TrunBox.Sample();
        MoofBox.TrunBox.Sample sample2 = new MoofBox.TrunBox.Sample();

        trun.addSample(sample1);
        trun.addSample(sample2);

        // Serialize
        byte[] data = trun.serialize();
        assertNotNull(data);

        // Deserialize
        MoofBox.TrunBox deserialized = new MoofBox.TrunBox();
        deserialized.deserialize(ByteBuffer.wrap(data));

        // Verify
        assertEquals(2, deserialized.getSampleCount());
        assertNotNull(deserialized.getFirstSampleFlags());
        assertTrue(deserialized.getFirstSampleFlags().isSyncSample());

        // First sample should inherit first_sample_flags
        assertNotNull(deserialized.getSamples().get(0).getSampleFlags());
        assertTrue(deserialized.getSamples().get(0).isSyncSample());

        // Second sample should have null flags (no per-sample flags)
        assertNull(deserialized.getSamples().get(1).getSampleFlags());
    }

    @Test
    @DisplayName("Test TrunBox with version 1 and signed composition time offset")
    void testTrunBoxVersion1() throws IOException {
        // Create a trun box with version 1 for signed composition time offset
        MoofBox.TrunBox trun = new MoofBox.TrunBox();
        trun.setVersion(1);
        trun.setTrunFlags(0x000801); // data_offset + composition_time_offset
        trun.setSampleCount(1);
        trun.setDataOffset(100);

        // Create a sample with negative composition time offset
        MoofBox.TrunBox.Sample sample = new MoofBox.TrunBox.Sample();
        sample.setCompositionTimeOffset(-500); // Negative offset (B-frames)

        trun.addSample(sample);

        // Serialize
        byte[] data = trun.serialize();
        assertNotNull(data);

        // Deserialize
        MoofBox.TrunBox deserialized = new MoofBox.TrunBox();
        deserialized.deserialize(ByteBuffer.wrap(data));

        // Verify
        assertEquals(1, deserialized.getVersion());
        assertEquals(1, deserialized.getSampleCount());
        assertEquals(-500, deserialized.getSamples().get(0).getCompositionTimeOffset());
    }

    @Test
    @DisplayName("Test SampleFlags toString format")
    void testSampleFlagsToString() {
        SampleFlags flags = SampleFlags.createSyncSampleFlags();
        String str = flags.toString();

        assertNotNull(str);
        assertTrue(str.contains("sync=yes"));
        assertTrue(str.contains("dependsOn=2"));
    }

    @Test
    @DisplayName("Test TrunBox with multiple samples having different flags")
    void testMultipleSamplesWithDifferentFlags() throws IOException {
        // Create a trun box with 5 samples - typical GOP pattern: I-B-B-P-P
        MoofBox.TrunBox trun = new MoofBox.TrunBox();
        trun.setVersion(0);
        trun.setTrunFlags(0x000701); // data_offset + duration + size + flags
        trun.setSampleCount(5);
        trun.setDataOffset(300);

        // I-frame (sync sample)
        MoofBox.TrunBox.Sample iFrame = new MoofBox.TrunBox.Sample();
        iFrame.setDuration(3000);
        iFrame.setSize(50000);
        iFrame.setSampleFlags(SampleFlags.createSyncSampleFlags());

        // B-frames (non-sync, depends on others)
        MoofBox.TrunBox.Sample bFrame1 = new MoofBox.TrunBox.Sample();
        bFrame1.setDuration(3000);
        bFrame1.setSize(5000);
        bFrame1.setSampleFlags(SampleFlags.createNonSyncSampleFlags());

        MoofBox.TrunBox.Sample bFrame2 = new MoofBox.TrunBox.Sample();
        bFrame2.setDuration(3000);
        bFrame2.setSize(5000);
        bFrame2.setSampleFlags(SampleFlags.createNonSyncSampleFlags());

        // P-frames (non-sync, but others may depend on them)
        int pFrameFlags = (1 << 24) | (1 << 22) | (1 << 16); // depends=1, depended_on=1, non-sync=1
        MoofBox.TrunBox.Sample pFrame1 = new MoofBox.TrunBox.Sample();
        pFrame1.setDuration(3000);
        pFrame1.setSize(10000);
        pFrame1.setSampleFlags(new SampleFlags(pFrameFlags));

        MoofBox.TrunBox.Sample pFrame2 = new MoofBox.TrunBox.Sample();
        pFrame2.setDuration(3000);
        pFrame2.setSize(10000);
        pFrame2.setSampleFlags(new SampleFlags(pFrameFlags));

        trun.addSample(iFrame);
        trun.addSample(bFrame1);
        trun.addSample(bFrame2);
        trun.addSample(pFrame1);
        trun.addSample(pFrame2);

        // Serialize
        byte[] data = trun.serialize();
        assertNotNull(data);

        // Deserialize
        MoofBox.TrunBox deserialized = new MoofBox.TrunBox();
        deserialized.deserialize(ByteBuffer.wrap(data));

        // Verify
        assertEquals(5, deserialized.getSampleCount());
        assertEquals(5, deserialized.getSamples().size());

        // Verify I-frame
        assertTrue(deserialized.getSamples().get(0).isSyncSample());
        assertTrue(deserialized.getSamples().get(0).isIndependent());
        assertEquals(50000, deserialized.getSamples().get(0).getSize());

        // Verify B-frames
        assertFalse(deserialized.getSamples().get(1).isSyncSample());
        assertFalse(deserialized.getSamples().get(1).isIndependent());
        assertFalse(deserialized.getSamples().get(2).isSyncSample());

        // Verify P-frames
        assertFalse(deserialized.getSamples().get(3).isSyncSample());
        assertFalse(deserialized.getSamples().get(3).isIndependent());
        assertTrue(deserialized.getSamples().get(3).getSampleFlags().isDependedUpon());
    }

    @Test
    @DisplayName("Test raw flags value round-trip")
    void testRawFlagsRoundTrip() {
        int originalFlags = 0x12345678;
        SampleFlags sampleFlags = new SampleFlags(originalFlags);

        assertEquals(originalFlags, sampleFlags.getFlags());
    }
}
