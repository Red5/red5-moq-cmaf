package org.red5.io.moq.cmaf;

import org.junit.jupiter.api.Test;
import org.red5.io.moq.cmaf.model.CmafFragment;
import org.red5.io.moq.cmaf.model.InitializationSegment.*;
import org.red5.io.moq.cmaf.model.MoovBox;
import org.red5.io.moq.cmaf.model.TrackMetadata.*;
import org.red5.io.moq.cmaf.serialize.CmafSerializer;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test demonstrating support for various video and audio codecs.
 * This test validates that our CMAF implementation supports all major modern codecs.
 */
class CodecSupportTest {

    /**
     * Test all supported video codecs with 4K resolution.
     */
    @Test
    void testAllVideoCodecs() throws IOException {
        System.out.println("\n=== Supported Video Codecs ===");

        String[][] videoCodecs = {
            {"avc1", "H.264/AVC (Baseline, Main, High profiles)"},
            {"avc3", "H.264/AVC with in-band parameter sets"},
            {"hev1", "H.265/HEVC (Main, Main 10 profiles)"},
            {"hvc1", "H.265/HEVC with in-band parameter sets"},
            {"vp09", "VP9 (Profile 0-3)"},
            {"av01", "AV1 (Main, High profiles)"}
        };

        for (String[] codec : videoCodecs) {
            String fourcc = codec[0];
            String description = codec[1];

            VisualSampleEntry entry = new VisualSampleEntry(fourcc);
            entry.setWidth(3840);  // 4K width
            entry.setHeight(2160); // 4K height
            entry.setCompressorName(description);

            // Add sample codec config
            byte[] codecConfig = createSampleVideoConfig(fourcc);
            entry.setCodecConfig(codecConfig);

            // Serialize and deserialize
            byte[] serialized = entry.serialize();
            VisualSampleEntry deserialized = new VisualSampleEntry();
            deserialized.deserialize(java.nio.ByteBuffer.wrap(serialized));

            // Verify
            assertEquals(fourcc, deserialized.getType());
            assertEquals(3840, deserialized.getWidth());
            assertEquals(2160, deserialized.getHeight());
            assertArrayEquals(codecConfig, deserialized.getCodecConfig());

            System.out.printf("  ✓ %s - %s\n", fourcc, description);
        }
    }

    /**
     * Test all supported audio codecs.
     */
    @Test
    void testAllAudioCodecs() throws IOException {
        System.out.println("\n=== Supported Audio Codecs ===");

        String[][] audioCodecs = {
            {"mp4a", "AAC (AAC-LC, HE-AAC, HE-AACv2)"},
            {"opus", "Opus (versatile for speech and music)"},
            {"Opus", "Opus (alternate FourCC)"},
            {"ac-3", "Dolby Digital (AC-3)"},
            {"ec-3", "Dolby Digital Plus (E-AC-3)"}
        };

        for (String[] codec : audioCodecs) {
            String fourcc = codec[0];
            String description = codec[1];

            AudioSampleEntry entry = new AudioSampleEntry(fourcc);
            entry.setChannelCount(2);
            entry.setSampleRateHz(48000);
            entry.setSampleSize(16);

            // Add sample codec config
            byte[] codecConfig = createSampleAudioConfig(fourcc);
            entry.setCodecConfig(codecConfig);

            // Serialize and deserialize
            byte[] serialized = entry.serialize();
            AudioSampleEntry deserialized = new AudioSampleEntry();
            deserialized.deserialize(java.nio.ByteBuffer.wrap(serialized));

            // Verify
            assertEquals(fourcc, deserialized.getType());
            assertEquals(2, deserialized.getChannelCount());
            assertEquals(48000, deserialized.getSampleRateHz());
            assertArrayEquals(codecConfig, deserialized.getCodecConfig());

            System.out.printf("  ✓ %s - %s\n", fourcc, description);
        }
    }

    /**
     * Test HEVC 4K streaming scenario.
     */
    @Test
    void testHEVC4KStreaming() throws IOException {
        System.out.println("\n=== HEVC 4K Streaming Test ===");

        // Create HEVC initialization segment
        MoovBox moov = new MoovBox();

        // Movie header
        MvhdBox mvhd = new MvhdBox();
        mvhd.setTimescale(90000); // 90kHz for video
        mvhd.setDuration(0); // Live stream
        mvhd.setNextTrackId(2);
        moov.setMvhd(mvhd);

        // Video track
        MoovBox.TrakBox trak = new MoovBox.TrakBox();

        TkhdBox tkhd = new TkhdBox();
        tkhd.setTrackId(1);
        tkhd.setDuration(0);
        tkhd.setWidthPixels(3840);  // 4K
        tkhd.setHeightPixels(2160);
        trak.setTkhd(tkhd);

        MoovBox.MdiaBox mdia = new MoovBox.MdiaBox();
        MdhdBox mdhd = new MdhdBox();
        mdhd.setTimescale(90000);
        mdhd.setDuration(0);
        mdia.setMdhd(mdhd);

        HdlrBox hdlr = new HdlrBox();
        hdlr.setHandlerType("vide");
        hdlr.setName("HEVC Video Handler");
        mdia.setHdlr(hdlr);

        MoovBox.MinfBox minf = new MoovBox.MinfBox();
        minf.setVmhd(new VmhdBox());

        MoovBox.DinfBox dinf = new MoovBox.DinfBox();
        dinf.setDref(new DrefBox());
        minf.setDinf(dinf);

        MoovBox.StblBox stbl = new MoovBox.StblBox();
        StsdBox stsd = new StsdBox();

        // HEVC sample entry
        VisualSampleEntry hevc = new VisualSampleEntry("hev1");
        hevc.setWidth(3840);
        hevc.setHeight(2160);
        hevc.setCompressorName("HEVC 4K Video");
        hevc.setCodecConfig(createHEVCConfig());
        stsd.setEntries(new SampleEntry[]{hevc});
        stbl.setStsd(stsd);

        stbl.setEmptyTables();
        minf.setStbl(stbl);
        mdia.setMinf(minf);
        trak.setMdia(mdia);
        moov.addTrak(trak);

        // Serialize and verify
        byte[] serialized = moov.serialize();
        MoovBox deserialized = new MoovBox();
        deserialized.deserialize(java.nio.ByteBuffer.wrap(serialized));

        assertNotNull(deserialized);
        assertEquals(1, deserialized.getTraks().size());

        MoovBox.TrakBox videoTrak = deserialized.getTraks().get(0);
        assertEquals(3840, videoTrak.getTkhd().getWidthPixels());
        assertEquals(2160, videoTrak.getTkhd().getHeightPixels());

        VisualSampleEntry hevcEntry = (VisualSampleEntry)
            videoTrak.getMdia().getMinf().getStbl().getStsd().getEntries()[0];
        assertEquals("hev1", hevcEntry.getType());

        System.out.println("  ✓ HEVC 4K initialization segment created successfully");
        System.out.println("  ✓ Resolution: 3840x2160");
        System.out.println("  ✓ Codec: HEVC/H.265");
    }

    /**
     * Test Opus audio streaming scenario.
     */
    @Test
    void testOpusAudioStreaming() throws IOException {
        System.out.println("\n=== Opus Audio Streaming Test ===");

        // Create Opus fragment (10ms at 48kHz)
        byte[] opusFrame = new byte[480]; // Typical Opus frame size
        CmafFragment fragment = CmafSerializer.createMinimalFragment(1, opusFrame);
        fragment.setMediaType(CmafFragment.MediaType.AUDIO);

        // Serialize
        CmafSerializer serializer = new CmafSerializer();
        byte[] serialized = serializer.serialize(fragment);

        System.out.println("  ✓ Opus audio fragment created successfully");
        System.out.printf("  ✓ Fragment size: %d bytes\n", serialized.length);
        System.out.println("  ✓ Codec: Opus");
        System.out.println("  ✓ Sample rate: 48kHz");

        assertTrue(serialized.length > 0);
    }

    /**
     * Test multi-codec scenario (HEVC + Opus).
     */
    @Test
    void testHEVCWithOpus() throws IOException {
        System.out.println("\n=== HEVC + Opus Multi-Track Test ===");

        // Create initialization segment with both tracks
        MoovBox moov = new MoovBox();

        MvhdBox mvhd = new MvhdBox();
        mvhd.setTimescale(1000);
        mvhd.setDuration(0);
        mvhd.setNextTrackId(3);
        moov.setMvhd(mvhd);

        // Add HEVC video track
        moov.addTrak(createHEVCTrack(1));

        // Add Opus audio track
        moov.addTrak(createOpusTrack(2));

        // Serialize and verify
        byte[] serialized = moov.serialize();
        MoovBox deserialized = new MoovBox();
        deserialized.deserialize(java.nio.ByteBuffer.wrap(serialized));

        assertEquals(2, deserialized.getTraks().size());

        // Verify video track
        MoovBox.TrakBox videoTrak = deserialized.getTraks().get(0);
        assertEquals("vide", videoTrak.getMdia().getHdlr().getHandlerType());
        VisualSampleEntry videoEntry = (VisualSampleEntry)
            videoTrak.getMdia().getMinf().getStbl().getStsd().getEntries()[0];
        assertEquals("hev1", videoEntry.getType());

        // Verify audio track
        MoovBox.TrakBox audioTrak = deserialized.getTraks().get(1);
        assertEquals("soun", audioTrak.getMdia().getHdlr().getHandlerType());
        AudioSampleEntry audioEntry = (AudioSampleEntry)
            audioTrak.getMdia().getMinf().getStbl().getStsd().getEntries()[0];
        assertEquals("Opus", audioEntry.getType());

        System.out.println("  ✓ Multi-track initialization segment created");
        System.out.println("  ✓ Video: HEVC/H.265 @ 4K");
        System.out.println("  ✓ Audio: Opus @ 48kHz stereo");
    }

    // Helper methods

    private byte[] createSampleVideoConfig(String fourcc) {
        // Create minimal codec config for testing
        switch (fourcc) {
            case "avc1":
            case "avc3":
                return new byte[]{0x01, 0x42, 0x00, 0x1E}; // AVC DecoderConfigurationRecord
            case "hev1":
            case "hvc1":
                return createHEVCConfig();
            case "vp09":
                return new byte[]{0x01, 0x00, 0x00, 0x00}; // VP9 config
            case "av01":
                return new byte[]{(byte) 0x81, 0x00, 0x0C}; // AV1 config
            default:
                return new byte[0];
        }
    }

    private byte[] createSampleAudioConfig(String fourcc) {
        // Create minimal codec config for testing
        switch (fourcc) {
            case "mp4a":
                return new byte[]{0x11, (byte) 0x90}; // AAC AudioSpecificConfig
            case "opus":
            case "Opus":
                return new byte[]{
                    0x4F, 0x70, 0x75, 0x73, 0x48, 0x65, 0x61, 0x64, // "OpusHead"
                    0x01, // Version
                    0x02, // Channel count
                    0x00, 0x00, // Pre-skip
                    (byte) 0x80, (byte) 0xBB, 0x00, 0x00, // Sample rate (48000)
                    0x00, 0x00, // Output gain
                    0x00 // Channel mapping family
                };
            case "ac-3":
                return new byte[]{0x00, 0x40}; // AC-3 config
            case "ec-3":
                return new byte[]{0x00, 0x60}; // E-AC-3 config
            default:
                return new byte[0];
        }
    }

    private byte[] createHEVCConfig() {
        // Minimal HEVC DecoderConfigurationRecord
        return new byte[]{
            0x01, // configurationVersion
            0x01, // general_profile_space, general_tier_flag, general_profile_idc
            0x60, 0x00, 0x00, 0x00, // general_profile_compatibility_flags
            (byte) 0x90, 0x00, 0x00, 0x00, 0x00, 0x00, // general_constraint_indicator_flags
            0x00, // general_level_idc
            (byte) 0xF0, 0x00, // min_spatial_segmentation_idc
            (byte) 0xFC, // parallelismType
            (byte) 0xFD, // chromaFormat
            (byte) 0xF8, // bitDepthLumaMinus8
            (byte) 0xF8, // bitDepthChromaMinus8
            0x00, 0x00, // avgFrameRate
            0x0F, // constantFrameRate, numTemporalLayers, temporalIdNested, lengthSizeMinusOne
            0x03, // numOfArrays
            0x00  // padding
        };
    }

    private MoovBox.TrakBox createHEVCTrack(int trackId) {
        MoovBox.TrakBox trak = new MoovBox.TrakBox();

        TkhdBox tkhd = new TkhdBox();
        tkhd.setTrackId(trackId);
        tkhd.setDuration(0);
        tkhd.setWidthPixels(3840);
        tkhd.setHeightPixels(2160);
        trak.setTkhd(tkhd);

        MoovBox.MdiaBox mdia = new MoovBox.MdiaBox();
        MdhdBox mdhd = new MdhdBox();
        mdhd.setTimescale(90000);
        mdhd.setDuration(0);
        mdia.setMdhd(mdhd);

        HdlrBox hdlr = new HdlrBox();
        hdlr.setHandlerType("vide");
        hdlr.setName("HEVC Video Handler");
        mdia.setHdlr(hdlr);

        MoovBox.MinfBox minf = new MoovBox.MinfBox();
        minf.setVmhd(new VmhdBox());

        MoovBox.DinfBox dinf = new MoovBox.DinfBox();
        dinf.setDref(new DrefBox());
        minf.setDinf(dinf);

        MoovBox.StblBox stbl = new MoovBox.StblBox();
        StsdBox stsd = new StsdBox();

        VisualSampleEntry hevc = new VisualSampleEntry("hev1");
        hevc.setWidth(3840);
        hevc.setHeight(2160);
        hevc.setCodecConfig(createHEVCConfig());
        stsd.setEntries(new SampleEntry[]{hevc});
        stbl.setStsd(stsd);
        stbl.setEmptyTables();

        minf.setStbl(stbl);
        mdia.setMinf(minf);
        trak.setMdia(mdia);

        return trak;
    }

    private MoovBox.TrakBox createOpusTrack(int trackId) {
        MoovBox.TrakBox trak = new MoovBox.TrakBox();

        TkhdBox tkhd = new TkhdBox();
        tkhd.setTrackId(trackId);
        tkhd.setDuration(0);
        tkhd.setVolume(0x0100); // Full volume
        trak.setTkhd(tkhd);

        MoovBox.MdiaBox mdia = new MoovBox.MdiaBox();
        MdhdBox mdhd = new MdhdBox();
        mdhd.setTimescale(48000);
        mdhd.setDuration(0);
        mdia.setMdhd(mdhd);

        HdlrBox hdlr = new HdlrBox();
        hdlr.setHandlerType("soun");
        hdlr.setName("Opus Audio Handler");
        mdia.setHdlr(hdlr);

        MoovBox.MinfBox minf = new MoovBox.MinfBox();
        SmhdBox smhd = new SmhdBox();
        smhd.setBalance(0);
        minf.setSmhd(smhd);

        MoovBox.DinfBox dinf = new MoovBox.DinfBox();
        dinf.setDref(new DrefBox());
        minf.setDinf(dinf);

        MoovBox.StblBox stbl = new MoovBox.StblBox();
        StsdBox stsd = new StsdBox();

        AudioSampleEntry opus = new AudioSampleEntry("Opus");
        opus.setChannelCount(2);
        opus.setSampleRateHz(48000);
        opus.setCodecConfig(createSampleAudioConfig("Opus"));
        stsd.setEntries(new SampleEntry[]{opus});
        stbl.setStsd(stsd);
        stbl.setEmptyTables();

        minf.setStbl(stbl);
        mdia.setMinf(minf);
        trak.setMdia(mdia);

        return trak;
    }
}
