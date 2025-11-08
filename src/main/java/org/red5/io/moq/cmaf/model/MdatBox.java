package org.red5.io.moq.cmaf.model;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Media Data Box (mdat) - ISO/IEC 14496-12.
 * Contains the actual media data (audio, video, etc.).
 */
public class MdatBox extends Box {
    private byte[] data;

    public MdatBox() {
        super("mdat");
    }

    public MdatBox(byte[] data) {
        super("mdat");
        this.data = data;
        this.size = calculateSize();
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        this.size = calculateSize();
    }

    @Override
    protected long calculateSize() {
        return 8 + (data != null ? data.length : 0);
    }

    @Override
    public byte[] serialize() throws IOException {
        this.size = calculateSize();
        ByteBuffer buffer = ByteBuffer.allocate((int) size);

        // Write header
        writeHeader(buffer);

        // Write data
        if (data != null) {
            buffer.put(data);
        }

        return buffer.array();
    }

    @Override
    public void deserialize(ByteBuffer buffer) throws IOException {
        // Read header
        long boxSize = readHeader(buffer);

        // Read data
        int dataSize = (int) (boxSize - 8); // size - header
        if (dataSize > 0) {
            this.data = new byte[dataSize];
            buffer.get(this.data);
        }
    }

    @Override
    public String toString() {
        return "MdatBox{" +
                "type='" + type + "', " +
                "size=" + size + ", " +
                "dataLength=" + (data != null ? data.length : 0) +
                '}';
    }
}
