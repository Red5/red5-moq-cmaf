# MoQ CMAF Packaging Library

A pure Java 21 library for serialization and deserialization of media content for MoQ (Media over QUIC) Transport, supporting both:
- **CMAF** (Common Media Application Format) packaging according to the [MoQ CMAF Packaging specification](https://github.com/wilaw/moq-cmaf-packaging/blob/main/draft-wilaw-moq-cmafpackaging.md)
- **LOC** (Low Overhead Media Container) format according to [draft-ietf-moq-loc](https://datatracker.ietf.org/doc/html/draft-mzanaty-moq-loc-05)

## Features

### CMAF Support

- **Full ISO BMFF Support**: Parse and generate ISO Base Media File Format boxes including initialization segments
- **CMAF Fragment Handling**: Complete support for styp, moof, and mdat boxes
- **High Performance**: Pure Java implementation capable of handling 4K/8K video content
- **Serialization/Deserialization**: Convert between CMAF fragments and byte arrays
- **Media File I/O**: Read and write CMAF files for debugging
- **Multiple Media Types**: Support for audio, video, metadata, and other content types
- **Codec Support**: H.264/AVC, H.265/HEVC, VP9, AV1, AAC, Opus, Dolby Digital/Plus

### LOC Support

- **Low Overhead Format**: Minimal encapsulation overhead optimized for WebCodecs
- **Header Extensions**: Support for capture timestamp, video frame marking, audio level, and video config
- **Temporal/Spatial Layers**: Full support for SVC and simulcast video encoding
- **End-to-End Encryption Ready**: Metadata designed for relay operation with encrypted payloads
- **WebCodecs Compatible**: Direct mapping to EncodedAudioChunk and EncodedVideoChunk

### Common

- **MoQ Transport Ready**: Both formats designed for use with MoQ Transport protocol
- **Comprehensive Testing**: Full test coverage for both CMAF and LOC implementations

## Requirements

- Java 21 or later
- Maven 3.8+

## Building

### Compile and Package

Tests are included in the build process, to skip tests use `-DskipTests` flag.

```bash
mvn clean package
```

All 89 tests should pass with 100% success rate.

## Quick Start

### Creating a CMAF Fragment

```java
import org.red5.io.moq.cmaf.model.CmafFragment;
import org.red5.io.moq.cmaf.serialize.CmafSerializer;

// Create media data
byte[] mediaData = new byte[1024];
// ... fill with actual media data

// Create a CMAF fragment
CmafFragment fragment = CmafSerializer.createMinimalFragment(1, mediaData);
fragment.setGroupId(1);
fragment.setObjectId(1);
fragment.setMediaType(CmafFragment.MediaType.VIDEO);

// Serialize to bytes (ready for MoQ Transport)
CmafSerializer serializer = new CmafSerializer();
byte[] serialized = serializer.serialize(fragment);
```

### Deserializing a CMAF Fragment

```java
import org.red5.io.moq.cmaf.deserialize.CmafDeserializer;

// Receive bytes from MoQ Transport
byte[] receivedData = ...;

// Deserialize
CmafDeserializer deserializer = new CmafDeserializer();
CmafFragment fragment = deserializer.deserialize(receivedData);

// Access fragment data
long sequenceNumber = fragment.getSequenceNumber();
byte[] mediaData = fragment.getMdat().getData();
```

### File Operations (for Debugging)

```java
import org.red5.io.moq.cmaf.util.MediaFileWriter;
import org.red5.io.moq.cmaf.util.MediaFileReader;
import java.nio.file.Paths;

// Write fragment to file
MediaFileWriter writer = new MediaFileWriter();
writer.writeFragment(fragment, Paths.get("output.cmaf"));

// Read fragment from file
MediaFileReader reader = new MediaFileReader();
CmafFragment readFragment = reader.readFragment(Paths.get("output.cmaf"));

// Analyze file
reader.analyzeFile(Paths.get("output.cmaf"));
```

### Creating a LOC Object (Audio)

```java
import org.red5.io.moq.loc.model.LocObject;
import org.red5.io.moq.loc.serialize.LocSerializer;

// Create audio data (e.g., 10ms Opus frame at 48kHz)
byte[] audioData = new byte[480];
long timestamp = System.currentTimeMillis() * 1000; // microseconds

// Create LOC object with metadata
LocObject obj = LocSerializer.createMinimalAudioObject(audioData, timestamp);
obj.setGroupId(100);
obj.setObjectId(1);
obj.setAudioLevel(true, 45); // voice activity, level 45

// Serialize for MoQ Transport
LocSerializer serializer = new LocSerializer();
byte[] headerExtensions = serializer.serializeHeaderExtensions(obj);
byte[] payload = serializer.getPayload(obj);
```

### Creating a LOC Object (Video)

```java
import org.red5.io.moq.loc.model.LocObject;
import org.red5.io.moq.loc.serialize.LocSerializer;

// Create video data (key frame)
byte[] videoData = new byte[8192];
long timestamp = System.currentTimeMillis() * 1000;

// Create LOC object for independent frame
LocObject obj = LocSerializer.createMinimalVideoObject(videoData, timestamp, true);
obj.setGroupId(50);
obj.setObjectId(0); // First object in group (key frame)

// Add video config (codec extradata)
byte[] configData = new byte[]{0x01, 0x42, 0xC0, 0x1E}; // H.264 avcC
obj.setVideoConfig(configData);

// Serialize
LocSerializer serializer = new LocSerializer();
byte[] serialized = serializer.serialize(obj);
```

### Deserializing a LOC Object

```java
import org.red5.io.moq.loc.deserialize.LocDeserializer;
import org.red5.io.moq.loc.model.LocObject;

// Receive from MoQ Transport (header extensions and payload separated)
byte[] headerExtensions = ...;
byte[] payload = ...;

// Deserialize
LocDeserializer deserializer = new LocDeserializer();
LocObject obj = deserializer.deserialize(
    headerExtensions,
    payload,
    LocObject.MediaType.VIDEO
);

// Access metadata
if (obj.isIndependentFrame()) {
    System.out.println("Key frame received");
}
long captureTime = obj.getCaptureTimestamp().getCaptureTimestampMicros();
```

## Architecture

### Package Structure

```
org.red5.io.moq
├── cmaf/               # CMAF format support
│   ├── model/              # Data structures for ISO BMFF boxes
│   │   ├── Box.java
│   │   ├── StypBox.java
│   │   ├── MoofBox.java
│   │   ├── MdatBox.java
│   │   ├── MoovBox.java            # Initialization segment support
│   │   ├── InitializationSegment.java
│   │   ├── TrackMetadata.java      # Video/audio track metadata
│   │   ├── SampleFlags.java        # ISO BMFF sample flags
│   │   └── CmafFragment.java
│   ├── serialize/          # Serialization to bytes
│   │   └── CmafSerializer.java
│   ├── deserialize/        # Deserialization from bytes
│   │   └── CmafDeserializer.java
│   └── util/              # File I/O utilities
│       ├── MediaFileReader.java
│       └── MediaFileWriter.java
└── loc/                # LOC format support
    ├── model/              # LOC data structures
    │   ├── LocObject.java
    │   ├── LocHeaderExtension.java
    │   ├── CaptureTimestampExtension.java
    │   ├── VideoFrameMarkingExtension.java
    │   ├── AudioLevelExtension.java
    │   └── VideoConfigExtension.java
    ├── serialize/          # LOC serialization
    │   └── LocSerializer.java
    └── deserialize/        # LOC deserialization
        └── LocDeserializer.java
```

## Specification Compliance

### MoQ CMAF Packaging

This library implements [draft-wilaw-moq-cmafpackaging](https://github.com/wilaw/moq-cmaf-packaging/blob/main/draft-wilaw-moq-cmafpackaging.md):

- **Fragment-to-Group Mapping**: Complete CMAF fragments map to single MoQ objects
- **ISO BMFF Structure**: Each object contains styp + moof + mdat boxes
- **Single Track**: One ISO BMFF track per object
- **Decode Order**: Content in decode order with increasing timestamps
- **Time Alignment**: Support for media time-aligned group numbers across tracks

### LOC (Low Overhead Media Container)

This library implements [draft-ietf-moq-loc](https://datatracker.ietf.org/doc/html/draft-mzanaty-moq-loc-05):

- **LOC Payload**: Direct mapping to WebCodecs EncodedAudioChunk/EncodedVideoChunk internal data
- **LOC Header Extensions**: Support for registered extensions (capture timestamp, video frame marking, audio level, video config)
- **Varint Encoding**: Efficient encoding for metadata values
- **Extension Types**: Support for both varint values (even IDs) and byte array values (odd IDs)
- **Relay-Friendly Metadata**: Metadata accessible without decrypting payloads

## Testing

The library includes comprehensive unit tests:

### Test Suites

**CMAF Tests:**

- `CmafFragmentTest`: Fragment serialization/deserialization, all box types
- `InitializationSegmentTest`: Initialization segment (ftyp + moov) support
- `MediaFileOperationsTest`: File I/O operations
- `PerformanceTest`: 4K/8K performance benchmarks
- `CodecSupportTest`: Codec validation (H.264, HEVC, VP9, AV1, AAC, Opus, etc.)

**LOC Tests:**

- `LocObjectTest`: LOC object serialization/deserialization, header extensions, and all metadata types

Run all tests:

```bash
mvn test
```

Run specific test suite:

```bash
mvn test -Dtest=LocObjectTest
mvn test -Dtest=CmafFragmentTest
mvn test -Dtest=PerformanceTest
```

## Performance

The pure Java implementation has been thoroughly tested and validated for high-performance media processing:

**Performance Benchmarks:**

- **4K Video @ 30fps @ 25 Mbps**: 2.94ms per fragment, 2.0 GB/s throughput
- **4K Video @ 60fps @ 50 Mbps**: 6.42ms per fragment, 1.9 GB/s throughput
- **8K Video @ 30fps @ 100 Mbps**: 13.7ms per fragment, 1.7 GB/s throughput
- **Multi-track**: 563 fragments/sec for simultaneous video+audio processing
- **Memory Efficient**: Minimal garbage collection overhead

**Codec Support:**

- Video: H.264/AVC, H.265/HEVC, VP9, AV1
- Audio: AAC, Opus, Dolby Digital/Plus

See `PerformanceTest.java` and `CodecSupportTest.java` for detailed benchmarks.

## License

Apache License 2.0 - See LICENSE file for details

## Contributing

Based on the MOQtail project. Contributions welcome!

## Diamond Sponsors

- Red5 - [red5.net](https://red5.net)

## References

### Specifications

- [MoQ CMAF Packaging Draft](https://github.com/wilaw/moq-cmaf-packaging/blob/main/draft-wilaw-moq-cmafpackaging.md)
- [LOC - Low Overhead Media Container (draft-ietf-moq-loc)](https://datatracker.ietf.org/doc/html/draft-mzanaty-moq-loc-05)
- [ISO Base Media File Format (ISO/IEC 14496-12)](https://www.iso.org/standard/74428.html)
- [CMAF (ISO/IEC 23000-19)](https://www.iso.org/standard/79106.html)
- [MoQ Transport](https://datatracker.ietf.org/wg/moq/about/)
- [WebCodecs](https://www.w3.org/TR/webcodecs/)
- [RFC9626 - Video Frame Marking](https://datatracker.ietf.org/doc/html/rfc9626)
- [RFC6464 - Audio Level Extension](https://datatracker.ietf.org/doc/html/rfc6464)

## Support

For issues, questions, or contributions, please open an issue on GitHub.
