package org.red5.io.moq.cmaf.util;

import org.red5.io.moq.cmaf.model.CmafFragment;
import org.red5.io.moq.cmaf.serialize.CmafSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Utility class for writing CMAF media files.
 * Supports writing single or multiple fragments for debugging purposes.
 */
public class MediaFileWriter {
    private static final Logger logger = LoggerFactory.getLogger(MediaFileWriter.class);
    private final CmafSerializer serializer;

    public MediaFileWriter() {
        this.serializer = new CmafSerializer();
    }

    /**
     * Write a single CMAF fragment to a file.
     *
     * @param fragment the CMAF fragment to write
     * @param filePath the path to write to
     * @throws IOException if writing fails
     */
    public void writeFragment(CmafFragment fragment, Path filePath) throws IOException {
        logger.info("Writing CMAF fragment to: {}", filePath);

        if (fragment == null) {
            throw new IllegalArgumentException("Fragment cannot be null");
        }

        // Create parent directories if needed
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }

        serializer.serializeToFile(fragment, filePath);

        logger.info("Successfully wrote fragment to: {}", filePath);
        logger.info("  File size: {} bytes", Files.size(filePath));
    }

    /**
     * Write multiple CMAF fragments to a single file.
     * Creates a valid multi-fragment MP4 file.
     *
     * @param fragments list of CMAF fragments to write
     * @param filePath the path to write to
     * @throws IOException if writing fails
     */
    public void writeFragments(List<CmafFragment> fragments, Path filePath) throws IOException {
        logger.info("Writing {} CMAF fragments to: {}", fragments.size(), filePath);

        if (fragments == null || fragments.isEmpty()) {
            throw new IllegalArgumentException("Fragments list cannot be null or empty");
        }

        // Create parent directories if needed
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }

        serializer.serializeMultipleFragmentsToFile(fragments, filePath);

        logger.info("Successfully wrote {} fragments to: {}", fragments.size(), filePath);
        logger.info("  File size: {} bytes", Files.size(filePath));
    }

    /**
     * Append a CMAF fragment to an existing file.
     *
     * @param fragment the CMAF fragment to append
     * @param filePath the path to append to
     * @throws IOException if writing fails
     */
    public void appendFragment(CmafFragment fragment, Path filePath) throws IOException {
        logger.info("Appending CMAF fragment to: {}", filePath);

        if (fragment == null) {
            throw new IllegalArgumentException("Fragment cannot be null");
        }

        // Create parent directories if needed
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }

        // Append to file
        try (OutputStream out = Files.newOutputStream(filePath,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            serializer.serializeToStream(fragment, out);
        }

        logger.info("Successfully appended fragment to: {}", filePath);
        logger.info("  New file size: {} bytes", Files.size(filePath));
    }

    /**
     * Write media data as CMAF fragments to a file.
     * Creates minimal CMAF fragments with the provided media data.
     *
     * @param mediaDataArray array of media data byte arrays
     * @param filePath the path to write to
     * @param mediaType the type of media (AUDIO, VIDEO, etc.)
     * @throws IOException if writing fails
     */
    public void writeMediaData(byte[][] mediaDataArray, Path filePath, CmafFragment.MediaType mediaType)
            throws IOException {
        logger.info("Writing {} media data chunks as CMAF fragments to: {}",
                mediaDataArray.length, filePath);

        if (mediaDataArray == null || mediaDataArray.length == 0) {
            throw new IllegalArgumentException("Media data array cannot be null or empty");
        }

        // Create fragments from media data
        List<CmafFragment> fragments = new java.util.ArrayList<>();
        for (int i = 0; i < mediaDataArray.length; i++) {
            CmafFragment fragment = CmafSerializer.createMinimalFragment(i + 1, mediaDataArray[i]);
            fragment.setMediaType(mediaType);
            fragments.add(fragment);
        }

        writeFragments(fragments, filePath);
    }

    /**
     * Create a debug file with human-readable fragment information.
     *
     * @param fragment the fragment to create debug info for
     * @param filePath the path to write the debug info to
     * @throws IOException if writing fails
     */
    public void writeDebugInfo(CmafFragment fragment, Path filePath) throws IOException {
        logger.info("Writing debug info to: {}", filePath);

        if (fragment == null) {
            throw new IllegalArgumentException("Fragment cannot be null");
        }

        // Create parent directories if needed
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== CMAF Fragment Debug Info ===\n\n");

        sb.append("Fragment Properties:\n");
        sb.append(String.format("  Group ID: %d\n", fragment.getGroupId()));
        sb.append(String.format("  Object ID: %d\n", fragment.getObjectId()));
        sb.append(String.format("  Media Type: %s\n", fragment.getMediaType()));
        sb.append(String.format("  Total Size: %d bytes\n", fragment.getTotalSize()));
        sb.append(String.format("  Sequence Number: %d\n", fragment.getSequenceNumber()));
        sb.append(String.format("  Base Media Decode Time: %d\n", fragment.getBaseMediaDecodeTime()));
        sb.append("\n");

        if (fragment.getStyp() != null) {
            sb.append("Segment Type Box (styp):\n");
            sb.append(String.format("  Size: %d bytes\n", fragment.getStyp().getSize()));
            sb.append(String.format("  Major Brand: %s\n", fragment.getStyp().getMajorBrand()));
            sb.append(String.format("  Minor Version: %d\n", fragment.getStyp().getMinorVersion()));
            sb.append(String.format("  Compatible Brands: %s\n", fragment.getStyp().getCompatibleBrands()));
            sb.append("\n");
        }

        if (fragment.getMoof() != null) {
            sb.append("Movie Fragment Box (moof):\n");
            sb.append(String.format("  Size: %d bytes\n", fragment.getMoof().getSize()));
            if (fragment.getMoof().getMfhd() != null) {
                sb.append(String.format("  Sequence Number: %d\n",
                        fragment.getMoof().getMfhd().getSequenceNumber()));
            }
            sb.append(String.format("  Track Fragments: %d\n", fragment.getMoof().getTrafs().size()));
            sb.append("\n");
        }

        if (fragment.getMdat() != null) {
            sb.append("Media Data Box (mdat):\n");
            sb.append(String.format("  Size: %d bytes\n", fragment.getMdat().getSize()));
            if (fragment.getMdat().getData() != null) {
                sb.append(String.format("  Data Length: %d bytes\n", fragment.getMdat().getData().length));
                // Show first few bytes as hex
                byte[] data = fragment.getMdat().getData();
                int previewLen = Math.min(32, data.length);
                sb.append("  Data Preview (hex): ");
                for (int i = 0; i < previewLen; i++) {
                    sb.append(String.format("%02X ", data[i]));
                }
                if (data.length > previewLen) {
                    sb.append("...");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        Files.writeString(filePath, sb.toString());
        logger.info("Successfully wrote debug info to: {}", filePath);
    }

    /**
     * Create a debug file with information about multiple fragments.
     *
     * @param fragments list of fragments to create debug info for
     * @param filePath the path to write the debug info to
     * @throws IOException if writing fails
     */
    public void writeDebugInfo(List<CmafFragment> fragments, Path filePath) throws IOException {
        logger.info("Writing debug info for {} fragments to: {}", fragments.size(), filePath);

        if (fragments == null || fragments.isEmpty()) {
            throw new IllegalArgumentException("Fragments list cannot be null or empty");
        }

        // Create parent directories if needed
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== CMAF Fragments Debug Info ===\n\n");
        sb.append(String.format("Total Fragments: %d\n\n", fragments.size()));

        long totalSize = 0;
        for (CmafFragment fragment : fragments) {
            totalSize += fragment.getTotalSize();
        }
        sb.append(String.format("Total Size: %d bytes\n\n", totalSize));

        for (int i = 0; i < fragments.size(); i++) {
            CmafFragment fragment = fragments.get(i);
            sb.append(String.format("--- Fragment %d ---\n", i + 1));
            sb.append(String.format("Sequence: %d, Decode Time: %d, Size: %d bytes\n",
                    fragment.getSequenceNumber(),
                    fragment.getBaseMediaDecodeTime(),
                    fragment.getTotalSize()));
            sb.append("\n");
        }

        Files.writeString(filePath, sb.toString());
        logger.info("Successfully wrote debug info to: {}", filePath);
    }
}
