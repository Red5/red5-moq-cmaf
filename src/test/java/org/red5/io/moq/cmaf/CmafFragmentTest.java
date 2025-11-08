package org.red5.io.moq.cmaf;

import org.red5.io.moq.cmaf.deserialize.CmafDeserializer;
import org.red5.io.moq.cmaf.model.*;
import org.red5.io.moq.cmaf.serialize.CmafSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CMAF fragment serialization and deserialization.
 */
class CmafFragmentTest {

    @Test
    @DisplayName("Test StypBox serialization and deserialization")
    void testStypBox() throws IOException {
        // Create a styp box
        StypBox styp = new StypBox("cmf2", 0, List.of("cmfc", "iso6"));

        // Serialize
        byte[] data = styp.serialize();
        assertNotNull(data);
        assertTrue(data.length > 0);

        // Deserialize
        StypBox deserialized = new StypBox();
        deserialized.deserialize(java.nio.ByteBuffer.wrap(data));

        // Verify
        assertEquals(styp.getMajorBrand(), deserialized.getMajorBrand());
        assertEquals(styp.getMinorVersion(), deserialized.getMinorVersion());
        assertEquals(styp.getCompatibleBrands().size(), deserialized.getCompatibleBrands().size());
    }

    @Test
    @DisplayName("Test MoofBox serialization and deserialization")
    void testMoofBox() throws IOException {
        // Create moof box
        MoofBox moof = new MoofBox();

        MoofBox.MfhdBox mfhd = new MoofBox.MfhdBox(1);
        moof.setMfhd(mfhd);

        MoofBox.TrafBox traf = new MoofBox.TrafBox();
        MoofBox.TfhdBox tfhd = new MoofBox.TfhdBox();
        tfhd.setTrackId(1);
        traf.setTfhd(tfhd);

        MoofBox.TfdtBox tfdt = new MoofBox.TfdtBox(1000);
        traf.setTfdt(tfdt);

        moof.addTraf(traf);

        // Serialize
        byte[] data = moof.serialize();
        assertNotNull(data);
        assertTrue(data.length > 0);

        // Deserialize
        MoofBox deserialized = new MoofBox();
        deserialized.deserialize(java.nio.ByteBuffer.wrap(data));

        // Verify
        assertNotNull(deserialized.getMfhd());
        assertEquals(moof.getMfhd().getSequenceNumber(), deserialized.getMfhd().getSequenceNumber());
        assertEquals(moof.getTrafs().size(), deserialized.getTrafs().size());
    }

    @Test
    @DisplayName("Test MdatBox serialization and deserialization")
    void testMdatBox() throws IOException {
        // Create mdat box
        byte[] mediaData = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        MdatBox mdat = new MdatBox(mediaData);

        // Serialize
        byte[] data = mdat.serialize();
        assertNotNull(data);
        assertTrue(data.length > 0);

        // Deserialize
        MdatBox deserialized = new MdatBox();
        deserialized.deserialize(java.nio.ByteBuffer.wrap(data));

        // Verify
        assertNotNull(deserialized.getData());
        assertArrayEquals(mediaData, deserialized.getData());
    }

    @Test
    @DisplayName("Test complete CMAF fragment serialization and deserialization")
    void testCmafFragment() throws IOException {
        // Create a complete fragment
        byte[] mediaData = new byte[128];
        for (int i = 0; i < mediaData.length; i++) {
            mediaData[i] = (byte) (i & 0xFF);
        }

        CmafFragment fragment = CmafSerializer.createMinimalFragment(1, mediaData);
        fragment.setGroupId(100);
        fragment.setObjectId(1);
        fragment.setMediaType(CmafFragment.MediaType.VIDEO);

        // Serialize
        CmafSerializer serializer = new CmafSerializer();
        byte[] data = serializer.serialize(fragment);
        assertNotNull(data);
        assertTrue(data.length > 0);

        // Deserialize
        CmafDeserializer deserializer = new CmafDeserializer();
        CmafFragment deserialized = deserializer.deserialize(data);

        // Verify
        assertNotNull(deserialized);
        assertNotNull(deserialized.getStyp());
        assertNotNull(deserialized.getMoof());
        assertNotNull(deserialized.getMdat());

        assertEquals(fragment.getSequenceNumber(), deserialized.getSequenceNumber());
        assertArrayEquals(mediaData, deserialized.getMdat().getData());
    }

    @Test
    @DisplayName("Test fragment validation")
    void testFragmentValidation() {
        CmafDeserializer deserializer = new CmafDeserializer();

        // Valid fragment
        byte[] mediaData = new byte[]{0x01, 0x02, 0x03};
        CmafFragment validFragment = CmafSerializer.createMinimalFragment(1, mediaData);
        assertTrue(deserializer.validate(validFragment));

        // Invalid fragment (missing styp)
        CmafFragment invalidFragment = new CmafFragment();
        invalidFragment.setMoof(new MoofBox());
        invalidFragment.setMdat(new MdatBox());
        assertFalse(deserializer.validate(invalidFragment));

        // Null fragment
        assertFalse(deserializer.validate(null));
    }

    @Test
    @DisplayName("Test multiple fragments")
    void testMultipleFragments() throws IOException {
        CmafSerializer serializer = new CmafSerializer();
        CmafDeserializer deserializer = new CmafDeserializer();

        // Create multiple fragments
        int fragmentCount = 5;
        byte[][] mediaDataArray = new byte[fragmentCount][];

        for (int i = 0; i < fragmentCount; i++) {
            mediaDataArray[i] = new byte[64];
            for (int j = 0; j < mediaDataArray[i].length; j++) {
                mediaDataArray[i][j] = (byte) ((i * 64 + j) & 0xFF);
            }
        }

        // Serialize each fragment
        byte[][] serializedFragments = new byte[fragmentCount][];
        for (int i = 0; i < fragmentCount; i++) {
            CmafFragment fragment = CmafSerializer.createMinimalFragment(i + 1, mediaDataArray[i]);
            serializedFragments[i] = serializer.serialize(fragment);
        }

        // Deserialize and verify
        for (int i = 0; i < fragmentCount; i++) {
            CmafFragment deserialized = deserializer.deserialize(serializedFragments[i]);
            assertNotNull(deserialized);
            assertEquals(i + 1, deserialized.getSequenceNumber());
            assertArrayEquals(mediaDataArray[i], deserialized.getMdat().getData());
        }
    }

    @Test
    @DisplayName("Test fragment with different media types")
    void testFragmentMediaTypes() throws IOException {
        CmafSerializer serializer = new CmafSerializer();

        byte[] mediaData = new byte[]{0x01, 0x02, 0x03};

        // Test all media types
        for (CmafFragment.MediaType mediaType : CmafFragment.MediaType.values()) {
            CmafFragment fragment = CmafSerializer.createMinimalFragment(1, mediaData);
            fragment.setMediaType(mediaType);

            byte[] data = serializer.serialize(fragment);
            assertNotNull(data);
            assertTrue(data.length > 0);
        }
    }

    @Test
    @DisplayName("Test empty media data")
    void testEmptyMediaData() throws IOException {
        byte[] emptyData = new byte[0];
        CmafFragment fragment = CmafSerializer.createMinimalFragment(1, emptyData);

        CmafSerializer serializer = new CmafSerializer();
        byte[] serialized = serializer.serialize(fragment);

        assertNotNull(serialized);

        CmafDeserializer deserializer = new CmafDeserializer();
        CmafFragment deserialized = deserializer.deserialize(serialized);

        assertNotNull(deserialized);
        assertNotNull(deserialized.getMdat());
    }

    @Test
    @DisplayName("Test large media data")
    void testLargeMediaData() throws IOException {
        // Create 1MB of media data
        byte[] largeData = new byte[1024 * 1024];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i & 0xFF);
        }

        CmafFragment fragment = CmafSerializer.createMinimalFragment(1, largeData);

        CmafSerializer serializer = new CmafSerializer();
        byte[] serialized = serializer.serialize(fragment);

        assertNotNull(serialized);

        CmafDeserializer deserializer = new CmafDeserializer();
        CmafFragment deserialized = deserializer.deserialize(serialized);

        assertNotNull(deserialized);
        assertArrayEquals(largeData, deserialized.getMdat().getData());
    }
}
