package org.red5.io.moq.cmaf;

import org.junit.jupiter.api.Test;
import org.red5.io.moq.cmaf.deserialize.CmafDeserializer;
import org.red5.io.moq.cmaf.model.CmafFragment;
import org.red5.io.moq.cmaf.serialize.CmafSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for CMAF serialization/deserialization.
 * Tests whether the Java implementation can handle high-throughput scenarios like 4K video.
 */
class PerformanceTest {

    /**
     * Test 4K video fragment processing.
     *
     * Typical 4K video characteristics:
     * - Resolution: 3840x2160
     * - Frame rate: 30fps or 60fps
     * - Bitrate: 15-50 Mbps (streaming), up to 100+ Mbps (high quality)
     * - Fragment duration: 1-2 seconds (typical for CMAF)
     *
     * At 25 Mbps (moderate 4K):
     * - 1 second fragment = 3.125 MB
     * - 2 second fragment = 6.25 MB
     *
     * At 50 Mbps (high quality 4K):
     * - 1 second fragment = 6.25 MB
     * - 2 second fragment = 12.5 MB
     */
    @Test
    void test4KVideoFragmentPerformance() throws IOException {
        System.out.println("\n=== 4K Video Fragment Performance Test ===");

        // Test different 4K scenarios
        test4KScenario("4K 30fps @ 15 Mbps (1s fragment)", 1_875_000, 100);  // 15 Mbps / 8 * 1s
        test4KScenario("4K 30fps @ 25 Mbps (2s fragment)", 6_250_000, 100);  // 25 Mbps / 8 * 2s
        test4KScenario("4K 60fps @ 50 Mbps (1s fragment)", 6_250_000, 100);  // 50 Mbps / 8 * 1s
        test4KScenario("4K 60fps @ 50 Mbps (2s fragment)", 12_500_000, 50);  // 50 Mbps / 8 * 2s
    }

    /**
     * Test 8K video fragment processing (extreme case).
     */
    @Test
    void test8KVideoFragmentPerformance() throws IOException {
        System.out.println("\n=== 8K Video Fragment Performance Test ===");

        // 8K @ 100 Mbps, 2s fragments
        test4KScenario("8K 30fps @ 100 Mbps (2s fragment)", 25_000_000, 50);  // 100 Mbps / 8 * 2s
    }

    /**
     * Test multi-track scenario (video + audio).
     */
    @Test
    void testMultiTrackPerformance() throws IOException {
        System.out.println("\n=== Multi-Track Performance Test ===");

        int iterations = 100;

        // 4K video track: 25 Mbps, 2s fragments
        byte[] videoData = new byte[6_250_000];

        // Audio track: 256 kbps, 2s fragments
        byte[] audioData = new byte[64_000];

        CmafSerializer serializer = new CmafSerializer();
        CmafDeserializer deserializer = new CmafDeserializer();

        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            // Process video fragment
            CmafFragment videoFragment = CmafSerializer.createMinimalFragment(i + 1, videoData);
            videoFragment.setMediaType(CmafFragment.MediaType.VIDEO);
            byte[] videoSerialized = serializer.serialize(videoFragment);
            CmafFragment videoDeserialized = deserializer.deserialize(videoSerialized);

            // Process audio fragment
            CmafFragment audioFragment = CmafSerializer.createMinimalFragment(i + 1, audioData);
            audioFragment.setMediaType(CmafFragment.MediaType.AUDIO);
            byte[] audioSerialized = serializer.serialize(audioFragment);
            CmafFragment audioDeserialized = deserializer.deserialize(audioSerialized);

            assertNotNull(videoDeserialized);
            assertNotNull(audioDeserialized);
        }

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;

        int totalFragments = iterations * 2; // video + audio
        double fragmentsPerSecond = totalFragments / (durationMs / 1000.0);
        double totalDataMB = (iterations * (videoData.length + audioData.length)) / (1024.0 * 1024.0);
        double throughputMBps = totalDataMB / (durationMs / 1000.0);

        System.out.printf("Multi-track (4K video + audio): %d fragments in %.2f ms\n", totalFragments, durationMs);
        System.out.printf("  Throughput: %.2f fragments/sec, %.2f MB/s\n", fragmentsPerSecond, throughputMBps);
        System.out.printf("  Average per fragment: %.3f ms\n", durationMs / totalFragments);

        // Should process at least 50 fragments per second (both tracks combined)
        assertTrue(fragmentsPerSecond > 50,
            String.format("Too slow: %.2f fragments/sec (expected > 50)", fragmentsPerSecond));
    }

    /**
     * Test sustained high throughput (stress test).
     */
    @Test
    void testSustainedThroughput() throws IOException {
        System.out.println("\n=== Sustained Throughput Test ===");

        // Simulate 10 seconds of 4K streaming at 30fps, 2-second fragments
        // 5 fragments total, 25 Mbps bitrate
        int fragmentCount = 5;
        int fragmentSizeBytes = 6_250_000; // 2s at 25 Mbps

        CmafSerializer serializer = new CmafSerializer();
        CmafDeserializer deserializer = new CmafDeserializer();

        List<Long> fragmentTimes = new ArrayList<>();

        for (int i = 0; i < fragmentCount; i++) {
            byte[] mediaData = new byte[fragmentSizeBytes];

            long startTime = System.nanoTime();

            CmafFragment fragment = CmafSerializer.createMinimalFragment(i + 1, mediaData);
            fragment.setMediaType(CmafFragment.MediaType.VIDEO);
            byte[] serialized = serializer.serialize(fragment);
            CmafFragment deserialized = deserializer.deserialize(serialized);

            long endTime = System.nanoTime();
            long durationNs = endTime - startTime;
            fragmentTimes.add(durationNs);

            assertNotNull(deserialized);
        }

        // Calculate statistics
        double avgTimeMs = fragmentTimes.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
        long maxTimeNs = fragmentTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minTimeNs = fragmentTimes.stream().mapToLong(Long::longValue).min().orElse(0);

        System.out.printf("Sustained throughput test (%d fragments, %.2f MB each):\n",
            fragmentCount, fragmentSizeBytes / (1024.0 * 1024.0));
        System.out.printf("  Average: %.3f ms/fragment\n", avgTimeMs);
        System.out.printf("  Min: %.3f ms\n", minTimeNs / 1_000_000.0);
        System.out.printf("  Max: %.3f ms\n", maxTimeNs / 1_000_000.0);

        // For real-time processing, average should be well under 2000ms (fragment duration)
        assertTrue(avgTimeMs < 100,
            String.format("Too slow for real-time: %.3f ms average (should be < 100ms)", avgTimeMs));
    }

    /**
     * Test concurrent fragment processing.
     */
    @Test
    void testConcurrentProcessing() throws IOException, InterruptedException {
        System.out.println("\n=== Concurrent Processing Test ===");

        int threadCount = 4; // Simulate 4 concurrent streams
        int fragmentsPerThread = 25;
        int fragmentSize = 6_250_000; // 2s at 25 Mbps

        Thread[] threads = new Thread[threadCount];
        long[] threadTimes = new long[threadCount];

        long startTime = System.nanoTime();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                try {
                    CmafSerializer serializer = new CmafSerializer();
                    CmafDeserializer deserializer = new CmafDeserializer();

                    long threadStart = System.nanoTime();

                    for (int i = 0; i < fragmentsPerThread; i++) {
                        byte[] mediaData = new byte[fragmentSize];
                        CmafFragment fragment = CmafSerializer.createMinimalFragment(i + 1, mediaData);
                        byte[] serialized = serializer.serialize(fragment);
                        CmafFragment deserialized = deserializer.deserialize(serialized);
                        assertNotNull(deserialized);
                    }

                    threadTimes[threadId] = System.nanoTime() - threadStart;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            threads[t].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        long endTime = System.nanoTime();
        double totalTimeMs = (endTime - startTime) / 1_000_000.0;

        int totalFragments = threadCount * fragmentsPerThread;
        double fragmentsPerSecond = totalFragments / (totalTimeMs / 1000.0);

        System.out.printf("Concurrent processing (%d threads, %d fragments each):\n", threadCount, fragmentsPerThread);
        System.out.printf("  Total time: %.2f ms\n", totalTimeMs);
        System.out.printf("  Total throughput: %.2f fragments/sec\n", fragmentsPerSecond);
        System.out.printf("  Per-thread average: %.2f ms\n",
            java.util.Arrays.stream(threadTimes).average().orElse(0) / 1_000_000.0);

        // Should handle concurrent streams efficiently
        assertTrue(fragmentsPerSecond > 50,
            String.format("Concurrent throughput too low: %.2f fragments/sec", fragmentsPerSecond));
    }

    /**
     * Helper method to test a specific 4K scenario.
     */
    private void test4KScenario(String description, int fragmentSizeBytes, int iterations) throws IOException {
        byte[] mediaData = new byte[fragmentSizeBytes];
        CmafSerializer serializer = new CmafSerializer();
        CmafDeserializer deserializer = new CmafDeserializer();

        // Warmup
        for (int i = 0; i < 10; i++) {
            CmafFragment fragment = CmafSerializer.createMinimalFragment(1, mediaData);
            byte[] serialized = serializer.serialize(fragment);
            deserializer.deserialize(serialized);
        }

        // Actual test
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            CmafFragment fragment = CmafSerializer.createMinimalFragment(i + 1, mediaData);
            fragment.setMediaType(CmafFragment.MediaType.VIDEO);
            byte[] serialized = serializer.serialize(fragment);
            CmafFragment deserialized = deserializer.deserialize(serialized);
            assertNotNull(deserialized);
        }

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        double avgPerFragmentMs = durationMs / iterations;
        double fragmentsPerSecond = iterations / (durationMs / 1000.0);
        double totalDataMB = (iterations * fragmentSizeBytes) / (1024.0 * 1024.0);
        double throughputMBps = totalDataMB / (durationMs / 1000.0);

        System.out.printf("%s:\n", description);
        System.out.printf("  Fragment size: %.2f MB\n", fragmentSizeBytes / (1024.0 * 1024.0));
        System.out.printf("  %d iterations in %.2f ms\n", iterations, durationMs);
        System.out.printf("  Average: %.3f ms/fragment\n", avgPerFragmentMs);
        System.out.printf("  Throughput: %.2f fragments/sec, %.2f MB/s\n", fragmentsPerSecond, throughputMBps);

        // Performance assertions
        // Should process each fragment in under 50ms for real-time capability
        assertTrue(avgPerFragmentMs < 50,
            String.format("Too slow: %.3f ms/fragment (expected < 50ms)", avgPerFragmentMs));

        // Should handle at least 20 fragments per second
        assertTrue(fragmentsPerSecond > 20,
            String.format("Too slow: %.2f fragments/sec (expected > 20)", fragmentsPerSecond));
    }

    /**
     * Memory allocation test - ensure we're not creating excessive garbage.
     */
    @Test
    void testMemoryEfficiency() throws IOException {
        System.out.println("\n=== Memory Efficiency Test ===");

        int iterations = 1000;
        int fragmentSize = 1_000_000; // 1 MB fragments

        Runtime runtime = Runtime.getRuntime();

        // Force GC before test
        System.gc();
        Thread.yield();

        long memBefore = runtime.totalMemory() - runtime.freeMemory();

        CmafSerializer serializer = new CmafSerializer();
        CmafDeserializer deserializer = new CmafDeserializer();

        for (int i = 0; i < iterations; i++) {
            byte[] mediaData = new byte[fragmentSize];
            CmafFragment fragment = CmafSerializer.createMinimalFragment(i + 1, mediaData);
            byte[] serialized = serializer.serialize(fragment);
            CmafFragment deserialized = deserializer.deserialize(serialized);
            assertNotNull(deserialized);
        }

        // Force GC after test
        System.gc();
        Thread.yield();

        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long memDelta = memAfter - memBefore;

        System.out.printf("Memory efficiency (%d fragments of %.2f MB):\n", iterations, fragmentSize / (1024.0 * 1024.0));
        System.out.printf("  Memory before: %.2f MB\n", memBefore / (1024.0 * 1024.0));
        System.out.printf("  Memory after: %.2f MB\n", memAfter / (1024.0 * 1024.0));
        System.out.printf("  Delta: %.2f MB\n", memDelta / (1024.0 * 1024.0));

        // Should not accumulate excessive memory (allowing for some GC overhead)
        double memDeltaMB = memDelta / (1024.0 * 1024.0);
        assertTrue(memDeltaMB < 100,
            String.format("Excessive memory accumulation: %.2f MB", memDeltaMB));
    }
}
