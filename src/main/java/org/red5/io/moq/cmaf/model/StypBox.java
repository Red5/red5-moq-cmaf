package org.red5.io.moq.cmaf.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Segment Type Box (styp) - ISO/IEC 14496-12.
 * Similar to 'ftyp' but used for media segments.
 * Contains brand information for CMAF segments.
 */
public class StypBox extends Box {
    private String majorBrand;
    private long minorVersion;
    private List<String> compatibleBrands;

    public StypBox() {
        super("styp");
        this.compatibleBrands = new ArrayList<>();
    }

    public StypBox(String majorBrand, long minorVersion, List<String> compatibleBrands) {
        super("styp");
        this.majorBrand = majorBrand;
        this.minorVersion = minorVersion;
        this.compatibleBrands = compatibleBrands != null ? compatibleBrands : new ArrayList<>();
        this.size = calculateSize();
    }

    public String getMajorBrand() {
        return majorBrand;
    }

    public void setMajorBrand(String majorBrand) {
        this.majorBrand = majorBrand;
    }

    public long getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(long minorVersion) {
        this.minorVersion = minorVersion;
    }

    public List<String> getCompatibleBrands() {
        return compatibleBrands;
    }

    public void setCompatibleBrands(List<String> compatibleBrands) {
        this.compatibleBrands = compatibleBrands;
    }

    public void addCompatibleBrand(String brand) {
        this.compatibleBrands.add(brand);
    }

    @Override
    protected long calculateSize() {
        // 8 (header) + 4 (major_brand) + 4 (minor_version) + 4 * compatible_brands.length
        return 8 + 4 + 4 + (compatibleBrands.size() * 4L);
    }

    @Override
    public byte[] serialize() throws IOException {
        this.size = calculateSize();
        ByteBuffer buffer = ByteBuffer.allocate((int) size);

        // Write header
        writeHeader(buffer);

        // Write major brand
        buffer.put(majorBrand.getBytes(StandardCharsets.US_ASCII));

        // Write minor version
        buffer.putInt((int) minorVersion);

        // Write compatible brands
        for (String brand : compatibleBrands) {
            buffer.put(brand.getBytes(StandardCharsets.US_ASCII));
        }

        return buffer.array();
    }

    @Override
    public void deserialize(ByteBuffer buffer) throws IOException {
        // Read header
        readHeader(buffer);

        // Read major brand
        byte[] brandBytes = new byte[4];
        buffer.get(brandBytes);
        this.majorBrand = new String(brandBytes, StandardCharsets.US_ASCII);

        // Read minor version
        this.minorVersion = Integer.toUnsignedLong(buffer.getInt());

        // Read compatible brands
        this.compatibleBrands = new ArrayList<>();
        int remainingBytes = (int) (size - 16); // size - (header + major_brand + minor_version)
        while (remainingBytes >= 4) {
            buffer.get(brandBytes);
            compatibleBrands.add(new String(brandBytes, StandardCharsets.US_ASCII));
            remainingBytes -= 4;
        }
    }

    @Override
    public String toString() {
        return "StypBox{" +
                "type='" + type + "', " +
                "size=" + size + ", " +
                "majorBrand='" + majorBrand + "', " +
                "minorVersion=" + minorVersion + ", " +
                "compatibleBrands=" + compatibleBrands +
                '}';
    }
}
