package org.red5.io.moq.cmaf;

import org.junit.jupiter.api.Test;
import org.red5.io.moq.cmaf.model.*;
import org.red5.io.moq.cmaf.model.InitializationSegment.*;
import org.red5.io.moq.cmaf.model.TrackMetadata.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CMAF initialization segment boxes (ftyp, moov hierarchy).
 * These boxes are used to create the initialization segment that precedes
 * media fragments in a CMAF stream.
 */
class InitializationSegmentTest {

    @Test
    void testFtypBoxSerialization() throws IOException {
        // Create ftyp box with CMAF brands
        FtypBox ftyp = new FtypBox("cmfc", 0, List.of("cmf2", "iso6", "isom"));

        // Serialize
        byte[] serialized = ftyp.serialize();
        ByteBuffer buffer = ByteBuffer.wrap(serialized);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Deserialize
        FtypBox deserialized = new FtypBox();
        deserialized.deserialize(buffer);

        // Verify
        assertEquals("cmfc", deserialized.getMajorBrand());
        assertEquals(0, deserialized.getMinorVersion());
        assertEquals(3, deserialized.getCompatibleBrands().size());
        assertTrue(deserialized.getCompatibleBrands().contains("cmf2"));
        assertTrue(deserialized.getCompatibleBrands().contains("iso6"));
        assertTrue(deserialized.getCompatibleBrands().contains("isom"));
    }

    @Test
    void testMvhdBoxSerialization() throws IOException {
        // Create mvhd box
        MvhdBox mvhd = new MvhdBox();
        mvhd.setTimescale(1000);
        mvhd.setDuration(60000); // 60 seconds at 1000 Hz
        mvhd.setNextTrackId(2);

        // Serialize
        byte[] serialized = mvhd.serialize();
        ByteBuffer buffer = ByteBuffer.wrap(serialized);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Deserialize
        MvhdBox deserialized = new MvhdBox();
        deserialized.deserialize(buffer);

        // Verify
        assertEquals(1000, deserialized.getTimescale());
        assertEquals(60000, deserialized.getDuration());
        assertEquals(2, deserialized.getNextTrackId());
    }

    @Test
    void testHdlrBoxVideoHandler() throws IOException {
        // Create video handler
        HdlrBox hdlr = new HdlrBox();
        hdlr.setHandlerType("vide");
        hdlr.setName("VideoHandler");

        // Serialize
        byte[] serialized = hdlr.serialize();
        ByteBuffer buffer = ByteBuffer.wrap(serialized);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Deserialize
        HdlrBox deserialized = new HdlrBox();
        deserialized.deserialize(buffer);

        // Verify
        assertEquals("vide", deserialized.getHandlerType());
        assertEquals("VideoHandler", deserialized.getName());
    }

    @Test
    void testHdlrBoxAudioHandler() throws IOException {
        // Create audio handler
        HdlrBox hdlr = new HdlrBox();
        hdlr.setHandlerType("soun");
        hdlr.setName("SoundHandler");

        // Serialize
        byte[] serialized = hdlr.serialize();
        ByteBuffer buffer = ByteBuffer.wrap(serialized);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Deserialize
        HdlrBox deserialized = new HdlrBox();
        deserialized.deserialize(buffer);

        // Verify
        assertEquals("soun", deserialized.getHandlerType());
        assertEquals("SoundHandler", deserialized.getName());
    }

    @Test
    void testMdhdBoxSerialization() throws IOException {
        // Create mdhd box
        MdhdBox mdhd = new MdhdBox();
        mdhd.setTimescale(90000); // Common for video
        mdhd.setDuration(5400000); // 60 seconds at 90000 Hz
        mdhd.setLanguage(0x15C7); // "und" (undetermined)

        // Serialize
        byte[] serialized = mdhd.serialize();
        ByteBuffer buffer = ByteBuffer.wrap(serialized);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Deserialize
        MdhdBox deserialized = new MdhdBox();
        deserialized.deserialize(buffer);

        // Verify
        assertEquals(90000, deserialized.getTimescale());
        assertEquals(5400000, deserialized.getDuration());
        assertEquals(0x15C7, deserialized.getLanguage());
    }

    @Test
    void testVmhdBoxSerialization() throws IOException {
        // Create vmhd box (video media header)
        VmhdBox vmhd = new VmhdBox();

        // Serialize
        byte[] serialized = vmhd.serialize();
        ByteBuffer buffer = ByteBuffer.wrap(serialized);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Deserialize
        VmhdBox deserialized = new VmhdBox();
        deserialized.deserialize(buffer);

        // vmhd has no specific fields to verify, just ensure it serializes/deserializes
        assertNotNull(deserialized);
    }

    @Test
    void testSmhdBoxSerialization() throws IOException {
        // Create smhd box (sound media header)
        SmhdBox smhd = new SmhdBox();
        smhd.setBalance(0); // Center balance

        // Serialize
        byte[] serialized = smhd.serialize();
        ByteBuffer buffer = ByteBuffer.wrap(serialized);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Deserialize
        SmhdBox deserialized = new SmhdBox();
        deserialized.deserialize(buffer);

        // Verify
        assertEquals(0, deserialized.getBalance());
    }

    @Test
    void testDrefBoxSerialization() throws IOException {
        // Create dref box (data reference)
        DrefBox dref = new DrefBox();

        // Serialize
        byte[] serialized = dref.serialize();
        ByteBuffer buffer = ByteBuffer.wrap(serialized);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Deserialize
        DrefBox deserialized = new DrefBox();
        deserialized.deserialize(buffer);

        // Verify entry count is 1 (self-referencing url)
        assertEquals(1, deserialized.getEntryCount());
    }

    @Test
    void testCompleteMoovBoxWithVideoTrack() throws IOException {
        // Create complete moov box with video track
        MoovBox moov = createVideoMoovBox();

        // Serialize
        byte[] serialized = moov.serialize();
        ByteBuffer buffer = ByteBuffer.wrap(serialized);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Deserialize
        MoovBox deserialized = new MoovBox();
        deserialized.deserialize(buffer);

        // Verify moov structure
        assertNotNull(deserialized.getMvhd());
        assertEquals(1000, deserialized.getMvhd().getTimescale());
        assertEquals(1, deserialized.getTraks().size());

        // Verify trak structure
        MoovBox.TrakBox trak = deserialized.getTraks().get(0);
        assertNotNull(trak.getTkhd());
        assertEquals(1, trak.getTkhd().getTrackId());
        assertEquals(1920, trak.getTkhd().getWidthPixels());
        assertEquals(1080, trak.getTkhd().getHeightPixels());

        // Verify mdia structure
        MoovBox.MdiaBox mdia = trak.getMdia();
        assertNotNull(mdia.getMdhd());
        assertEquals(90000, mdia.getMdhd().getTimescale());
        assertNotNull(mdia.getHdlr());
        assertEquals("vide", mdia.getHdlr().getHandlerType());

        // Verify minf structure
        MoovBox.MinfBox minf = mdia.getMinf();
        assertNotNull(minf.getVmhd()); // Video has vmhd
        assertNull(minf.getSmhd()); // Video doesn't have smhd
        assertNotNull(minf.getDinf());
        assertNotNull(minf.getStbl());

        // Verify stbl has stsd with video sample entry
        assertNotNull(minf.getStbl().getStsd());
        assertEquals(1, minf.getStbl().getStsd().getEntryCount());
        assertTrue(minf.getStbl().getStsd().getEntries()[0] instanceof VisualSampleEntry);
        VisualSampleEntry entry = (VisualSampleEntry) minf.getStbl().getStsd().getEntries()[0];
        assertEquals("avc1", entry.getFormat());
        assertEquals(1920, entry.getWidth());
        assertEquals(1080, entry.getHeight());
    }

    @Test
    void testCompleteMoovBoxWithAudioTrack() throws IOException {
        // Create complete moov box with audio track
        MoovBox moov = createAudioMoovBox();

        // Serialize
        byte[] serialized = moov.serialize();
        ByteBuffer buffer = ByteBuffer.wrap(serialized);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Deserialize
        MoovBox deserialized = new MoovBox();
        deserialized.deserialize(buffer);

        // Verify moov structure
        assertNotNull(deserialized.getMvhd());
        assertEquals(1, deserialized.getTraks().size());

        // Verify trak structure
        MoovBox.TrakBox trak = deserialized.getTraks().get(0);
        assertNotNull(trak.getTkhd());
        assertEquals(2, trak.getTkhd().getTrackId());

        // Verify mdia structure
        MoovBox.MdiaBox mdia = trak.getMdia();
        assertNotNull(mdia.getMdhd());
        assertEquals(48000, mdia.getMdhd().getTimescale());
        assertNotNull(mdia.getHdlr());
        assertEquals("soun", mdia.getHdlr().getHandlerType());

        // Verify minf structure
        MoovBox.MinfBox minf = mdia.getMinf();
        assertNull(minf.getVmhd()); // Audio doesn't have vmhd
        assertNotNull(minf.getSmhd()); // Audio has smhd
        assertNotNull(minf.getDinf());
        assertNotNull(minf.getStbl());

        // Verify stbl has stsd with audio sample entry
        assertNotNull(minf.getStbl().getStsd());
        assertEquals(1, minf.getStbl().getStsd().getEntryCount());
        assertTrue(minf.getStbl().getStsd().getEntries()[0] instanceof AudioSampleEntry);
        AudioSampleEntry entry = (AudioSampleEntry) minf.getStbl().getStsd().getEntries()[0];
        assertEquals("mp4a", entry.getFormat());
        assertEquals(2, entry.getChannelCount());
        assertEquals(48000, entry.getSampleRateHz());
    }

    @Test
    void testMoovBoxWithMultipleTracks() throws IOException {
        // Create moov with both video and audio tracks
        MoovBox moov = new MoovBox();

        // Movie header
        MvhdBox mvhd = new MvhdBox();
        mvhd.setTimescale(1000);
        mvhd.setDuration(60000);
        mvhd.setNextTrackId(3); // Next would be track 3
        moov.setMvhd(mvhd);

        // Add video track
        moov.addTrak(createVideoTrack(1));

        // Add audio track
        moov.addTrak(createAudioTrack(2));

        // Serialize
        byte[] serialized = moov.serialize();
        ByteBuffer buffer = ByteBuffer.wrap(serialized);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Deserialize
        MoovBox deserialized = new MoovBox();
        deserialized.deserialize(buffer);

        // Verify we have 2 tracks
        assertEquals(2, deserialized.getTraks().size());

        // Verify video track
        MoovBox.TrakBox videoTrak = deserialized.getTraks().get(0);
        assertEquals(1, videoTrak.getTkhd().getTrackId());
        assertEquals("vide", videoTrak.getMdia().getHdlr().getHandlerType());

        // Verify audio track
        MoovBox.TrakBox audioTrak = deserialized.getTraks().get(1);
        assertEquals(2, audioTrak.getTkhd().getTrackId());
        assertEquals("soun", audioTrak.getMdia().getHdlr().getHandlerType());
    }

    @Test
    void testCompleteInitializationSegment() throws IOException {
        // Create complete initialization segment: ftyp + moov

        // Create ftyp
        FtypBox ftyp = new FtypBox("cmfc", 0, List.of("cmf2", "iso6", "isom"));

        // Create moov with video track
        MoovBox moov = createVideoMoovBox();

        // Serialize both
        byte[] ftypSerialized = ftyp.serialize();
        byte[] moovSerialized = moov.serialize();

        // Combine into single buffer
        ByteBuffer buffer = ByteBuffer.allocate(ftypSerialized.length + moovSerialized.length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(ftypSerialized);
        buffer.put(moovSerialized);
        buffer.flip();

        // Deserialize ftyp
        FtypBox deserializedFtyp = new FtypBox();
        deserializedFtyp.deserialize(buffer);

        // Deserialize moov
        MoovBox deserializedMoov = new MoovBox();
        deserializedMoov.deserialize(buffer);

        // Verify ftyp
        assertEquals("cmfc", deserializedFtyp.getMajorBrand());
        assertTrue(deserializedFtyp.getCompatibleBrands().contains("cmf2"));

        // Verify moov
        assertNotNull(deserializedMoov.getMvhd());
        assertEquals(1, deserializedMoov.getTraks().size());
    }

    @Test
    void testEmptySampleTables() throws IOException {
        // Create stbl with empty sample tables (required for fragmented files)
        MoovBox.StblBox stbl = new MoovBox.StblBox();

        // Create stsd with video entry
        StsdBox stsd = new StsdBox();
        VisualSampleEntry entry = new VisualSampleEntry();
        entry.setFormat("avc1");
        entry.setWidth(1920);
        entry.setHeight(1080);
        stsd.setEntries(new SampleEntry[]{entry});
        stbl.setStsd(stsd);

        // Create empty tables
        stbl.setStts(new MoovBox.SttsBox());
        stbl.setStsc(new MoovBox.StscBox());
        stbl.setStsz(new MoovBox.StszBox());
        stbl.setStco(new MoovBox.StcoBox());

        // Serialize
        byte[] serialized = stbl.serialize();
        ByteBuffer buffer = ByteBuffer.wrap(serialized);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Deserialize
        MoovBox.StblBox deserialized = new MoovBox.StblBox();
        deserialized.deserialize(buffer);

        // Verify stsd
        assertNotNull(deserialized.getStsd());
        assertEquals(1, deserialized.getStsd().getEntryCount());

        // Verify empty tables exist
        assertNotNull(deserialized.getStts());
        assertNotNull(deserialized.getStsc());
        assertNotNull(deserialized.getStsz());
        assertNotNull(deserialized.getStco());
    }

    // Helper methods to create test structures

    private MoovBox createVideoMoovBox() {
        MoovBox moov = new MoovBox();

        // Movie header
        MvhdBox mvhd = new MvhdBox();
        mvhd.setTimescale(1000);
        mvhd.setDuration(60000);
        mvhd.setNextTrackId(2);
        moov.setMvhd(mvhd);

        // Add video track
        moov.addTrak(createVideoTrack(1));

        return moov;
    }

    private MoovBox createAudioMoovBox() {
        MoovBox moov = new MoovBox();

        // Movie header
        MvhdBox mvhd = new MvhdBox();
        mvhd.setTimescale(1000);
        mvhd.setDuration(60000);
        mvhd.setNextTrackId(2);
        moov.setMvhd(mvhd);

        // Add audio track
        moov.addTrak(createAudioTrack(2));

        return moov;
    }

    private MoovBox.TrakBox createVideoTrack(int trackId) {
        MoovBox.TrakBox trak = new MoovBox.TrakBox();

        // Track header
        TkhdBox tkhd = new TkhdBox();
        tkhd.setTrackId(trackId);
        tkhd.setDuration(60000);
        tkhd.setWidthPixels(1920);
        tkhd.setHeightPixels(1080);
        trak.setTkhd(tkhd);

        // Media box
        MoovBox.MdiaBox mdia = new MoovBox.MdiaBox();

        // Media header
        MdhdBox mdhd = new MdhdBox();
        mdhd.setTimescale(90000); // Common for video
        mdhd.setDuration(5400000); // 60 seconds at 90000 Hz
        mdhd.setLanguage(0x15C7); // "und"
        mdia.setMdhd(mdhd);

        // Handler
        HdlrBox hdlr = new HdlrBox();
        hdlr.setHandlerType("vide");
        hdlr.setName("VideoHandler");
        mdia.setHdlr(hdlr);

        // Media information
        MoovBox.MinfBox minf = new MoovBox.MinfBox();
        minf.setVmhd(new VmhdBox());

        // Data information
        MoovBox.DinfBox dinf = new MoovBox.DinfBox();
        dinf.setDref(new DrefBox());
        minf.setDinf(dinf);

        // Sample table
        MoovBox.StblBox stbl = new MoovBox.StblBox();

        // Sample description
        StsdBox stsd = new StsdBox();
        VisualSampleEntry entry = new VisualSampleEntry();
        entry.setFormat("avc1");
        entry.setWidth(1920);
        entry.setHeight(1080);
        entry.setCodecConfig(new byte[]{0x01, 0x42, 0x00, 0x1E}); // Sample AVC config
        stsd.setEntries(new SampleEntry[]{entry});
        stbl.setStsd(stsd);

        // Empty sample tables (required for fragmented files)
        stbl.setStts(new MoovBox.SttsBox());
        stbl.setStsc(new MoovBox.StscBox());
        stbl.setStsz(new MoovBox.StszBox());
        stbl.setStco(new MoovBox.StcoBox());

        minf.setStbl(stbl);
        mdia.setMinf(minf);
        trak.setMdia(mdia);

        return trak;
    }

    private MoovBox.TrakBox createAudioTrack(int trackId) {
        MoovBox.TrakBox trak = new MoovBox.TrakBox();

        // Track header
        TkhdBox tkhd = new TkhdBox();
        tkhd.setTrackId(trackId);
        tkhd.setDuration(60000);
        tkhd.setVolume(0x0100); // Full volume (1.0 in 8.8 fixed point)
        trak.setTkhd(tkhd);

        // Media box
        MoovBox.MdiaBox mdia = new MoovBox.MdiaBox();

        // Media header
        MdhdBox mdhd = new MdhdBox();
        mdhd.setTimescale(48000); // Common for audio
        mdhd.setDuration(2880000); // 60 seconds at 48000 Hz
        mdhd.setLanguage(0x15C7); // "und"
        mdia.setMdhd(mdhd);

        // Handler
        HdlrBox hdlr = new HdlrBox();
        hdlr.setHandlerType("soun");
        hdlr.setName("SoundHandler");
        mdia.setHdlr(hdlr);

        // Media information
        MoovBox.MinfBox minf = new MoovBox.MinfBox();
        SmhdBox smhd = new SmhdBox();
        smhd.setBalance(0); // Center
        minf.setSmhd(smhd);

        // Data information
        MoovBox.DinfBox dinf = new MoovBox.DinfBox();
        dinf.setDref(new DrefBox());
        minf.setDinf(dinf);

        // Sample table
        MoovBox.StblBox stbl = new MoovBox.StblBox();

        // Sample description
        StsdBox stsd = new StsdBox();
        AudioSampleEntry entry = new AudioSampleEntry();
        entry.setFormat("mp4a");
        entry.setChannelCount(2);
        entry.setSampleRateHz(48000);
        entry.setCodecConfig(new byte[]{0x11, (byte) 0x90}); // Sample AAC config
        stsd.setEntries(new SampleEntry[]{entry});
        stbl.setStsd(stsd);

        // Empty sample tables (required for fragmented files)
        stbl.setStts(new MoovBox.SttsBox());
        stbl.setStsc(new MoovBox.StscBox());
        stbl.setStsz(new MoovBox.StszBox());
        stbl.setStco(new MoovBox.StcoBox());

        minf.setStbl(stbl);
        mdia.setMinf(minf);
        trak.setMdia(mdia);

        return trak;
    }
}
