package org.red5.io.moq.cmaf;

import org.red5.io.moq.cmaf.model.CmafFragment;
import org.red5.io.moq.cmaf.serialize.CmafSerializer;
import org.red5.io.moq.cmaf.util.MediaFileReader;
import org.red5.io.moq.cmaf.util.MediaFileWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for media file read/write operations.
 */
class MediaFileOperationsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Test write and read single fragment")
    void testWriteReadSingleFragment() throws IOException {
        MediaFileWriter writer = new MediaFileWriter();
        MediaFileReader reader = new MediaFileReader();

        // Create a fragment
        byte[] mediaData = new byte[256];
        for (int i = 0; i < mediaData.length; i++) {
            mediaData[i] = (byte) (i & 0xFF);
        }

        CmafFragment fragment = CmafSerializer.createMinimalFragment(1, mediaData);
        fragment.setMediaType(CmafFragment.MediaType.VIDEO);

        // Write to file
        Path outputFile = tempDir.resolve("test_fragment.cmaf");
        writer.writeFragment(fragment, outputFile);

        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);

        // Read from file
        CmafFragment readFragment = reader.readFragment(outputFile);

        assertNotNull(readFragment);
        assertEquals(fragment.getSequenceNumber(), readFragment.getSequenceNumber());
        assertArrayEquals(mediaData, readFragment.getMdat().getData());
    }

    @Test
    @DisplayName("Test write and read multiple fragments")
    void testWriteReadMultipleFragments() throws IOException {
        MediaFileWriter writer = new MediaFileWriter();
        MediaFileReader reader = new MediaFileReader();

        // Create multiple fragments
        int fragmentCount = 10;
        List<CmafFragment> fragments = new ArrayList<>();

        for (int i = 0; i < fragmentCount; i++) {
            byte[] mediaData = new byte[128];
            for (int j = 0; j < mediaData.length; j++) {
                mediaData[j] = (byte) ((i * 128 + j) & 0xFF);
            }

            CmafFragment fragment = CmafSerializer.createMinimalFragment(i + 1, mediaData);
            fragment.setMediaType(CmafFragment.MediaType.VIDEO);
            fragments.add(fragment);
        }

        // Write to file
        Path outputFile = tempDir.resolve("test_fragments.cmaf");
        writer.writeFragments(fragments, outputFile);

        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);

        // Read from file
        List<CmafFragment> readFragments = reader.readFragments(outputFile);

        assertNotNull(readFragments);
        assertEquals(fragmentCount, readFragments.size());

        for (int i = 0; i < fragmentCount; i++) {
            assertEquals(fragments.get(i).getSequenceNumber(), readFragments.get(i).getSequenceNumber());
            assertArrayEquals(fragments.get(i).getMdat().getData(), readFragments.get(i).getMdat().getData());
        }
    }

    @Test
    @DisplayName("Test append fragment")
    void testAppendFragment() throws IOException {
        MediaFileWriter writer = new MediaFileWriter();
        MediaFileReader reader = new MediaFileReader();

        Path outputFile = tempDir.resolve("test_append.cmaf");

        // Write first fragment
        byte[] data1 = new byte[]{0x01, 0x02, 0x03};
        CmafFragment fragment1 = CmafSerializer.createMinimalFragment(1, data1);
        writer.writeFragment(fragment1, outputFile);

        long sizeAfterFirst = Files.size(outputFile);

        // Append second fragment
        byte[] data2 = new byte[]{0x04, 0x05, 0x06};
        CmafFragment fragment2 = CmafSerializer.createMinimalFragment(2, data2);
        writer.appendFragment(fragment2, outputFile);

        long sizeAfterSecond = Files.size(outputFile);
        assertTrue(sizeAfterSecond > sizeAfterFirst);

        // Read both fragments
        List<CmafFragment> fragments = reader.readFragments(outputFile);
        assertEquals(2, fragments.size());
        assertEquals(1, fragments.get(0).getSequenceNumber());
        assertEquals(2, fragments.get(1).getSequenceNumber());
    }

    @Test
    @DisplayName("Test write media data as CMAF fragments")
    void testWriteMediaData() throws IOException {
        MediaFileWriter writer = new MediaFileWriter();
        MediaFileReader reader = new MediaFileReader();

        // Create media data array
        byte[][] mediaDataArray = new byte[5][];
        for (int i = 0; i < mediaDataArray.length; i++) {
            mediaDataArray[i] = new byte[64];
            for (int j = 0; j < mediaDataArray[i].length; j++) {
                mediaDataArray[i][j] = (byte) ((i * 64 + j) & 0xFF);
            }
        }

        // Write as CMAF fragments
        Path outputFile = tempDir.resolve("test_media_data.cmaf");
        writer.writeMediaData(mediaDataArray, outputFile, CmafFragment.MediaType.AUDIO);

        assertTrue(Files.exists(outputFile));

        // Read back
        List<CmafFragment> fragments = reader.readFragments(outputFile);
        assertEquals(mediaDataArray.length, fragments.size());

        for (int i = 0; i < mediaDataArray.length; i++) {
            assertArrayEquals(mediaDataArray[i], fragments.get(i).getMdat().getData());
        }
    }

    @Test
    @DisplayName("Test extract media data")
    void testExtractMediaData() throws IOException {
        MediaFileWriter writer = new MediaFileWriter();
        MediaFileReader reader = new MediaFileReader();

        // Create and write fragments
        int fragmentCount = 5;
        byte[][] originalData = new byte[fragmentCount][];

        for (int i = 0; i < fragmentCount; i++) {
            originalData[i] = new byte[32];
            for (int j = 0; j < originalData[i].length; j++) {
                originalData[i][j] = (byte) ((i * 32 + j) & 0xFF);
            }
        }

        Path outputFile = tempDir.resolve("test_extract.cmaf");
        writer.writeMediaData(originalData, outputFile, CmafFragment.MediaType.VIDEO);

        // Extract media data
        byte[][] extractedData = reader.extractMediaData(outputFile);

        assertNotNull(extractedData);
        assertEquals(fragmentCount, extractedData.length);

        for (int i = 0; i < fragmentCount; i++) {
            assertArrayEquals(originalData[i], extractedData[i]);
        }
    }

    @Test
    @DisplayName("Test write debug info")
    void testWriteDebugInfo() throws IOException {
        MediaFileWriter writer = new MediaFileWriter();

        byte[] mediaData = new byte[]{0x01, 0x02, 0x03, 0x04};
        CmafFragment fragment = CmafSerializer.createMinimalFragment(1, mediaData);
        fragment.setMediaType(CmafFragment.MediaType.VIDEO);
        fragment.setGroupId(100);
        fragment.setObjectId(1);

        // Write debug info
        Path debugFile = tempDir.resolve("debug_info.txt");
        writer.writeDebugInfo(fragment, debugFile);

        assertTrue(Files.exists(debugFile));
        String content = Files.readString(debugFile);

        assertTrue(content.contains("CMAF Fragment Debug Info"));
        assertTrue(content.contains("Group ID: 100"));
        assertTrue(content.contains("Object ID: 1"));
        assertTrue(content.contains("Media Type: VIDEO"));
    }

    @Test
    @DisplayName("Test write debug info for multiple fragments")
    void testWriteDebugInfoMultiple() throws IOException {
        MediaFileWriter writer = new MediaFileWriter();

        List<CmafFragment> fragments = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            byte[] mediaData = new byte[]{(byte) i, (byte) (i + 1), (byte) (i + 2)};
            CmafFragment fragment = CmafSerializer.createMinimalFragment(i + 1, mediaData);
            fragments.add(fragment);
        }

        Path debugFile = tempDir.resolve("debug_info_multiple.txt");
        writer.writeDebugInfo(fragments, debugFile);

        assertTrue(Files.exists(debugFile));
        String content = Files.readString(debugFile);

        assertTrue(content.contains("Total Fragments: 3"));
        assertTrue(content.contains("Fragment 1"));
        assertTrue(content.contains("Fragment 2"));
        assertTrue(content.contains("Fragment 3"));
    }

    @Test
    @DisplayName("Test validate CMAF file")
    void testValidateCmafFile() throws IOException {
        MediaFileWriter writer = new MediaFileWriter();
        MediaFileReader reader = new MediaFileReader();

        // Create valid CMAF file
        byte[] mediaData = new byte[]{0x01, 0x02, 0x03};
        CmafFragment fragment = CmafSerializer.createMinimalFragment(1, mediaData);

        Path validFile = tempDir.resolve("valid.cmaf");
        writer.writeFragment(fragment, validFile);

        assertTrue(reader.isValidCmafFile(validFile));

        // Create invalid file
        Path invalidFile = tempDir.resolve("invalid.cmaf");
        Files.writeString(invalidFile, "This is not a CMAF file");

        assertFalse(reader.isValidCmafFile(invalidFile));
    }

    @Test
    @DisplayName("Test analyze file")
    void testAnalyzeFile() throws IOException {
        MediaFileWriter writer = new MediaFileWriter();
        MediaFileReader reader = new MediaFileReader();

        // Create fragments
        List<CmafFragment> fragments = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            byte[] mediaData = new byte[100];
            CmafFragment fragment = CmafSerializer.createMinimalFragment(i + 1, mediaData);
            fragments.add(fragment);
        }

        Path outputFile = tempDir.resolve("analyze_test.cmaf");
        writer.writeFragments(fragments, outputFile);

        // Analyze should not throw exception
        assertDoesNotThrow(() -> reader.analyzeFile(outputFile));
    }
}
