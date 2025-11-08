package org.red5.io.moq.cmaf.deserialize;

import org.red5.io.moq.cmaf.model.CmafFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Deserializes CMAF fragments from byte arrays or files.
 * Implements the MoQ CMAF packaging specification for deserialization.
 */
public class CmafDeserializer {
    private static final Logger logger = LoggerFactory.getLogger(CmafDeserializer.class);

    /**
     * Deserialize a CMAF fragment from a byte array.
     *
     * @param data the byte array containing the serialized fragment
     * @return the deserialized CMAF fragment
     * @throws IOException if deserialization fails
     */
    public CmafFragment deserialize(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        logger.debug("Deserializing CMAF fragment from {} bytes", data.length);

        CmafFragment fragment = new CmafFragment();
        fragment.deserialize(data);

        logger.debug("Deserialized fragment: sequenceNumber={}, decodeTime={}, totalSize={}",
                fragment.getSequenceNumber(), fragment.getBaseMediaDecodeTime(), fragment.getTotalSize());

        return fragment;
    }

    /**
     * Deserialize a CMAF fragment from a file.
     *
     * @param inputPath the path to read the serialized data from
     * @return the deserialized CMAF fragment
     * @throws IOException if reading or deserialization fails
     */
    public CmafFragment deserializeFromFile(Path inputPath) throws IOException {
        if (inputPath == null) {
            throw new IllegalArgumentException("Input path cannot be null");
        }
        if (!Files.exists(inputPath)) {
            throw new IOException("File does not exist: " + inputPath);
        }

        logger.info("Deserializing CMAF fragment from file: {}", inputPath);

        byte[] data = Files.readAllBytes(inputPath);
        CmafFragment fragment = deserialize(data);

        logger.info("Successfully deserialized fragment from {}", inputPath);
        return fragment;
    }

    /**
     * Deserialize a CMAF fragment from an InputStream.
     *
     * @param inputStream the InputStream to read from
     * @return the deserialized CMAF fragment
     * @throws IOException if reading or deserialization fails
     */
    public CmafFragment deserializeFromStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        logger.debug("Deserializing CMAF fragment from stream");

        byte[] data = inputStream.readAllBytes();
        CmafFragment fragment = deserialize(data);

        logger.debug("Successfully deserialized fragment from stream");
        return fragment;
    }

    /**
     * Deserialize multiple CMAF fragments from a file.
     * This is useful for debugging and reading multi-fragment MP4 files.
     *
     * @param inputPath the path to read the serialized data from
     * @return list of deserialized CMAF fragments
     * @throws IOException if reading or deserialization fails
     */
    public List<CmafFragment> deserializeMultipleFragmentsFromFile(Path inputPath) throws IOException {
        if (inputPath == null) {
            throw new IllegalArgumentException("Input path cannot be null");
        }
        if (!Files.exists(inputPath)) {
            throw new IOException("File does not exist: " + inputPath);
        }

        logger.info("Deserializing multiple CMAF fragments from file: {}", inputPath);

        List<CmafFragment> fragments = new ArrayList<>();
        byte[] data = Files.readAllBytes(inputPath);

        int offset = 0;
        int fragmentCount = 0;

        while (offset < data.length - 8) {
            try {
                // Look for 'styp' box which indicates the start of a fragment
                int boxSize = ((data[offset] & 0xFF) << 24) |
                             ((data[offset + 1] & 0xFF) << 16) |
                             ((data[offset + 2] & 0xFF) << 8) |
                             (data[offset + 3] & 0xFF);

                if (boxSize <= 0 || boxSize > data.length - offset) {
                    logger.warn("Invalid box size {} at offset {}, stopping", boxSize, offset);
                    break;
                }

                String boxType = new String(data, offset + 4, 4);

                if ("styp".equals(boxType)) {
                    // Found a fragment starting with styp
                    // Find the end of this complete fragment (styp + moof + mdat)
                    int fragmentEnd = findFragmentEnd(data, offset);
                    if (fragmentEnd > offset && fragmentEnd <= data.length) {
                        byte[] fragmentData = new byte[fragmentEnd - offset];
                        System.arraycopy(data, offset, fragmentData, 0, fragmentData.length);

                        CmafFragment fragment = deserialize(fragmentData);
                        fragments.add(fragment);
                        fragmentCount++;

                        logger.debug("Deserialized fragment {}: size={} bytes", fragmentCount, fragmentData.length);
                        offset = fragmentEnd;
                    } else {
                        logger.warn("Could not find fragment end at offset {}, stopping", offset);
                        break;
                    }
                } else {
                    // Not a styp box, skip it
                    offset += boxSize;
                }
            } catch (Exception e) {
                logger.warn("Error deserializing fragment at offset {}: {}", offset, e.getMessage());
                break;
            }
        }

        logger.info("Successfully deserialized {} fragments from {}", fragments.size(), inputPath);
        return fragments;
    }

    /**
     * Find the end of a CMAF fragment (after mdat).
     *
     * @param data the data array
     * @param start the start offset
     * @return the end offset or -1 if not found
     */
    private int findFragmentEnd(byte[] data, int start) {
        int offset = start;
        boolean foundStyp = false;
        boolean foundMoof = false;
        boolean foundMdat = false;

        while (offset < data.length - 8) {
            int boxSize = ((data[offset] & 0xFF) << 24) |
                         ((data[offset + 1] & 0xFF) << 16) |
                         ((data[offset + 2] & 0xFF) << 8) |
                         (data[offset + 3] & 0xFF);

            if (boxSize <= 0 || boxSize > data.length - offset) {
                // Invalid box size
                return -1;
            }

            String boxType = new String(data, offset + 4, 4);

            if ("styp".equals(boxType)) {
                if (foundStyp) {
                    // Found the start of a new fragment, so the previous one ended
                    return offset;
                }
                foundStyp = true;
                offset += boxSize;
            } else if ("moof".equals(boxType)) {
                if (foundMoof && !foundMdat) {
                    // Found a new moof before mdat, which shouldn't happen in a valid fragment
                    return -1;
                }
                foundMoof = true;
                offset += boxSize;
            } else if ("mdat".equals(boxType)) {
                foundMdat = true;
                // mdat is typically the last box in a fragment
                return offset + boxSize;
            } else {
                // Unknown box, skip it
                offset += boxSize;
            }
        }

        return foundMdat ? offset : -1;
    }

    /**
     * Validate a CMAF fragment.
     *
     * @param fragment the fragment to validate
     * @return true if valid, false otherwise
     */
    public boolean validate(CmafFragment fragment) {
        if (fragment == null) {
            logger.warn("Fragment is null");
            return false;
        }

        if (fragment.getStyp() == null) {
            logger.warn("Fragment missing styp box");
            return false;
        }

        if (fragment.getMoof() == null) {
            logger.warn("Fragment missing moof box");
            return false;
        }

        if (fragment.getMdat() == null) {
            logger.warn("Fragment missing mdat box");
            return false;
        }

        if (fragment.getMoof().getMfhd() == null) {
            logger.warn("Fragment moof missing mfhd box");
            return false;
        }

        if (fragment.getMoof().getTrafs().isEmpty()) {
            logger.warn("Fragment moof has no traf boxes");
            return false;
        }

        logger.debug("Fragment validation passed");
        return true;
    }
}
