package org.red5.io.moq.cmaf.serialize;

import org.red5.io.moq.cmaf.model.CmafFragment;
import org.red5.io.moq.cmaf.model.MdatBox;
import org.red5.io.moq.cmaf.model.MoofBox;
import org.red5.io.moq.cmaf.model.StypBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Serializes CMAF fragments to byte arrays or files.
 * Implements the MoQ CMAF packaging specification for serialization.
 */
public class CmafSerializer {
    private static final Logger logger = LoggerFactory.getLogger(CmafSerializer.class);

    /**
     * Serialize a CMAF fragment to a byte array.
     *
     * @param fragment the CMAF fragment to serialize
     * @return byte array containing the serialized fragment
     * @throws IOException if serialization fails
     */
    public byte[] serialize(CmafFragment fragment) throws IOException {
        if (fragment == null) {
            throw new IllegalArgumentException("Fragment cannot be null");
        }

        logger.debug("Serializing CMAF fragment: groupId={}, objectId={}, mediaType={}",
                fragment.getGroupId(), fragment.getObjectId(), fragment.getMediaType());

        byte[] data = fragment.serialize();

        logger.debug("Serialized fragment size: {} bytes", data.length);
        return data;
    }

    /**
     * Serialize a CMAF fragment to a file.
     *
     * @param fragment the CMAF fragment to serialize
     * @param outputPath the path to write the serialized data
     * @throws IOException if writing fails
     */
    public void serializeToFile(CmafFragment fragment, Path outputPath) throws IOException {
        if (fragment == null) {
            throw new IllegalArgumentException("Fragment cannot be null");
        }
        if (outputPath == null) {
            throw new IllegalArgumentException("Output path cannot be null");
        }

        logger.info("Serializing CMAF fragment to file: {}", outputPath);

        byte[] data = serialize(fragment);
        Files.write(outputPath, data);

        logger.info("Successfully wrote {} bytes to {}", data.length, outputPath);
    }

    /**
     * Serialize a CMAF fragment to an OutputStream.
     *
     * @param fragment the CMAF fragment to serialize
     * @param outputStream the OutputStream to write to
     * @throws IOException if writing fails
     */
    public void serializeToStream(CmafFragment fragment, OutputStream outputStream) throws IOException {
        if (fragment == null) {
            throw new IllegalArgumentException("Fragment cannot be null");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("OutputStream cannot be null");
        }

        logger.debug("Serializing CMAF fragment to stream");

        byte[] data = serialize(fragment);
        outputStream.write(data);
        outputStream.flush();

        logger.debug("Successfully wrote {} bytes to stream", data.length);
    }

    /**
     * Serialize multiple CMAF fragments to a file (for debugging).
     * Creates a valid MP4 file with multiple fragments.
     *
     * @param fragments list of CMAF fragments
     * @param outputPath the path to write the serialized data
     * @throws IOException if writing fails
     */
    public void serializeMultipleFragmentsToFile(List<CmafFragment> fragments, Path outputPath) throws IOException {
        if (fragments == null || fragments.isEmpty()) {
            throw new IllegalArgumentException("Fragments list cannot be null or empty");
        }
        if (outputPath == null) {
            throw new IllegalArgumentException("Output path cannot be null");
        }

        logger.info("Serializing {} CMAF fragments to file: {}", fragments.size(), outputPath);

        try (OutputStream out = Files.newOutputStream(outputPath)) {
            for (int i = 0; i < fragments.size(); i++) {
                CmafFragment fragment = fragments.get(i);
                logger.debug("Serializing fragment {}/{}", i + 1, fragments.size());
                serializeToStream(fragment, out);
            }
        }

        logger.info("Successfully serialized {} fragments to {}", fragments.size(), outputPath);
    }

    /**
     * Create a minimal CMAF fragment for testing.
     *
     * @param sequenceNumber the fragment sequence number
     * @param mediaData the media data to include
     * @return a new CMAF fragment
     */
    public static CmafFragment createMinimalFragment(long sequenceNumber, byte[] mediaData) {
        // Create styp box
        StypBox styp = new StypBox("cmf2", 0, List.of("cmfc", "iso6"));

        // Create moof box with mfhd
        MoofBox moof = new MoofBox();
        MoofBox.MfhdBox mfhd = new MoofBox.MfhdBox(sequenceNumber);
        moof.setMfhd(mfhd);

        // Create traf
        MoofBox.TrafBox traf = new MoofBox.TrafBox();
        MoofBox.TfhdBox tfhd = new MoofBox.TfhdBox();
        tfhd.setTrackId(1);
        traf.setTfhd(tfhd);

        MoofBox.TfdtBox tfdt = new MoofBox.TfdtBox(sequenceNumber * 1000);
        traf.setTfdt(tfdt);

        moof.addTraf(traf);

        // Create mdat box
        MdatBox mdat = new MdatBox(mediaData);

        // Create fragment
        CmafFragment fragment = new CmafFragment(styp, moof, mdat);
        fragment.setGroupId(1);
        fragment.setObjectId(sequenceNumber);
        fragment.setMediaType(CmafFragment.MediaType.VIDEO);

        return fragment;
    }
}
