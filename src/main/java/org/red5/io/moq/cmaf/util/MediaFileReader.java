package org.red5.io.moq.cmaf.util;

import org.red5.io.moq.cmaf.deserialize.CmafDeserializer;
import org.red5.io.moq.cmaf.model.CmafFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Utility class for reading CMAF media files.
 * Supports reading single or multiple fragments for debugging purposes.
 */
public class MediaFileReader {
    private static final Logger logger = LoggerFactory.getLogger(MediaFileReader.class);
    private final CmafDeserializer deserializer;

    public MediaFileReader() {
        this.deserializer = new CmafDeserializer();
    }

    /**
     * Read a single CMAF fragment from a file.
     *
     * @param filePath the path to the media file
     * @return the deserialized CMAF fragment
     * @throws IOException if reading fails
     */
    public CmafFragment readFragment(Path filePath) throws IOException {
        logger.info("Reading CMAF fragment from: {}", filePath);

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }

        CmafFragment fragment = deserializer.deserializeFromFile(filePath);

        if (!deserializer.validate(fragment)) {
            logger.warn("Fragment validation failed for: {}", filePath);
        }

        logFragmentInfo(fragment);
        return fragment;
    }

    /**
     * Read multiple CMAF fragments from a file.
     *
     * @param filePath the path to the media file
     * @return list of deserialized CMAF fragments
     * @throws IOException if reading fails
     */
    public List<CmafFragment> readFragments(Path filePath) throws IOException {
        logger.info("Reading CMAF fragments from: {}", filePath);

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }

        List<CmafFragment> fragments = deserializer.deserializeMultipleFragmentsFromFile(filePath);

        logger.info("Read {} fragments from file", fragments.size());

        for (int i = 0; i < fragments.size(); i++) {
            logger.debug("Fragment {}:", i + 1);
            logFragmentInfo(fragments.get(i));
        }

        return fragments;
    }

    /**
     * Analyze a media file and print detailed information.
     *
     * @param filePath the path to the media file
     * @throws IOException if reading fails
     */
    public void analyzeFile(Path filePath) throws IOException {
        logger.info("Analyzing media file: {}", filePath);

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }

        long fileSize = Files.size(filePath);
        logger.info("File size: {} bytes", fileSize);

        List<CmafFragment> fragments = readFragments(filePath);

        logger.info("=== Media File Analysis ===");
        logger.info("Total fragments: {}", fragments.size());

        long totalMediaData = 0;
        for (int i = 0; i < fragments.size(); i++) {
            CmafFragment fragment = fragments.get(i);
            logger.info("Fragment {}: seq={}, decodeTime={}, size={} bytes",
                    i + 1,
                    fragment.getSequenceNumber(),
                    fragment.getBaseMediaDecodeTime(),
                    fragment.getTotalSize());

            if (fragment.getMdat() != null && fragment.getMdat().getData() != null) {
                totalMediaData += fragment.getMdat().getData().length;
            }
        }

        logger.info("Total media data: {} bytes", totalMediaData);
        logger.info("Overhead: {} bytes ({}%)",
                fileSize - totalMediaData,
                String.format("%.2f", (fileSize - totalMediaData) * 100.0 / fileSize));
    }

    /**
     * Extract media data from a CMAF file.
     *
     * @param filePath the path to the media file
     * @return array of media data byte arrays (one per fragment)
     * @throws IOException if reading fails
     */
    public byte[][] extractMediaData(Path filePath) throws IOException {
        List<CmafFragment> fragments = readFragments(filePath);

        byte[][] mediaData = new byte[fragments.size()][];
        for (int i = 0; i < fragments.size(); i++) {
            CmafFragment fragment = fragments.get(i);
            if (fragment.getMdat() != null) {
                mediaData[i] = fragment.getMdat().getData();
            } else {
                mediaData[i] = new byte[0];
            }
        }

        logger.info("Extracted media data from {} fragments", mediaData.length);
        return mediaData;
    }

    /**
     * Check if a file is a valid CMAF file.
     *
     * @param filePath the path to check
     * @return true if valid CMAF file, false otherwise
     */
    public boolean isValidCmafFile(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return false;
            }

            List<CmafFragment> fragments = readFragments(filePath);
            if (fragments.isEmpty()) {
                return false;
            }

            // Validate all fragments
            for (CmafFragment fragment : fragments) {
                if (!deserializer.validate(fragment)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Error validating CMAF file: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Log detailed information about a fragment.
     *
     * @param fragment the fragment to log
     */
    private void logFragmentInfo(CmafFragment fragment) {
        logger.debug("  Sequence: {}", fragment.getSequenceNumber());
        logger.debug("  Decode time: {}", fragment.getBaseMediaDecodeTime());
        logger.debug("  Total size: {} bytes", fragment.getTotalSize());

        if (fragment.getStyp() != null) {
            logger.debug("  Styp: brand={}, version={}, compatible={}",
                    fragment.getStyp().getMajorBrand(),
                    fragment.getStyp().getMinorVersion(),
                    fragment.getStyp().getCompatibleBrands());
        }

        if (fragment.getMoof() != null) {
            logger.debug("  Moof: trafs={}", fragment.getMoof().getTrafs().size());
        }

        if (fragment.getMdat() != null && fragment.getMdat().getData() != null) {
            logger.debug("  Mdat: {} bytes", fragment.getMdat().getData().length);
        }
    }
}
