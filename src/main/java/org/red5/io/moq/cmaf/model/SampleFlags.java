package org.red5.io.moq.cmaf.model;

/**
 * Sample flags as defined in ISO/IEC 14496-12 Section 8.8.3.1.
 *
 * Sample flags are a 32-bit field that provide information about the sample's characteristics:
 * - Whether it's a sync sample (key frame)
 * - Dependencies on other samples
 * - Leading samples
 * - Redundancy
 * - Degradation priority
 */
public class SampleFlags {

    // Raw 32-bit flags value
    private final int flags;

    // Sample dependency information (bits 4-11)
    private final int isLeading;              // bits 4-5: 0=unknown, 1=has dependency before ref I-picture, 2=not leading, 3=no dependency before ref I-picture
    private final int sampleDependsOn;        // bits 6-7: 0=unknown, 1=depends on others, 2=does not depend, 3=reserved
    private final int sampleIsDependedOn;     // bits 8-9: 0=unknown, 1=others depend on this, 2=no others depend, 3=reserved
    private final int sampleHasRedundancy;    // bits 10-11: 0=unknown, 1=has redundant coding, 2=no redundant coding, 3=reserved

    // Padding and sync information (bits 12-15)
    private final int samplePaddingValue;     // bits 12-14: padding value
    private final boolean sampleIsNonSync;    // bit 15: false=sync sample (key frame), true=non-sync sample

    // Degradation priority (bits 16-31)
    private final int sampleDegradationPriority; // bits 16-31: 0=no priority

    /**
     * Creates SampleFlags from a 32-bit flags value.
     *
     * @param flags The 32-bit flags value from the trun box
     */
    public SampleFlags(int flags) {
        this.flags = flags;

        // Parse bit fields according to ISO/IEC 14496-12
        // Note: bits are numbered from MSB (bit 31) to LSB (bit 0)
        this.isLeading = (flags >> 26) & 0x3;                    // bits 4-5 (from MSB: bits 26-27)
        this.sampleDependsOn = (flags >> 24) & 0x3;              // bits 6-7 (from MSB: bits 24-25)
        this.sampleIsDependedOn = (flags >> 22) & 0x3;           // bits 8-9 (from MSB: bits 22-23)
        this.sampleHasRedundancy = (flags >> 20) & 0x3;          // bits 10-11 (from MSB: bits 20-21)
        this.samplePaddingValue = (flags >> 17) & 0x7;           // bits 12-14 (from MSB: bits 17-19)
        this.sampleIsNonSync = ((flags >> 16) & 0x1) != 0;       // bit 15 (from MSB: bit 16)
        this.sampleDegradationPriority = flags & 0xFFFF;         // bits 16-31 (from MSB: bits 0-15)
    }

    /**
     * @return The raw 32-bit flags value
     */
    public int getFlags() {
        return flags;
    }

    /**
     * @return Is leading (0=unknown, 1=has dependency before ref I-picture, 2=not leading, 3=no dependency before ref I-picture)
     */
    public int getIsLeading() {
        return isLeading;
    }

    /**
     * @return Sample depends on (0=unknown, 1=depends on others, 2=does not depend, 3=reserved)
     */
    public int getSampleDependsOn() {
        return sampleDependsOn;
    }

    /**
     * @return Sample is depended on (0=unknown, 1=others depend on this, 2=no others depend, 3=reserved)
     */
    public int getSampleIsDependedOn() {
        return sampleIsDependedOn;
    }

    /**
     * @return Sample has redundancy (0=unknown, 1=has redundant coding, 2=no redundant coding, 3=reserved)
     */
    public int getSampleHasRedundancy() {
        return sampleHasRedundancy;
    }

    /**
     * @return Sample padding value
     */
    public int getSamplePaddingValue() {
        return samplePaddingValue;
    }

    /**
     * @return true if this is a non-sync sample (not a key frame), false if it's a sync sample (key frame)
     */
    public boolean isSampleNonSync() {
        return sampleIsNonSync;
    }

    /**
     * @return true if this is a sync sample (key frame), false if it's a non-sync sample
     */
    public boolean isSyncSample() {
        return !sampleIsNonSync;
    }

    /**
     * @return Sample degradation priority (0 = no priority)
     */
    public int getSampleDegradationPriority() {
        return sampleDegradationPriority;
    }

    /**
     * @return true if this sample does not depend on other samples (can be decoded independently)
     */
    public boolean isIndependent() {
        return sampleDependsOn == 2;
    }

    /**
     * @return true if other samples depend on this one
     */
    public boolean isDependedUpon() {
        return sampleIsDependedOn == 1;
    }

    @Override
    public String toString() {
        return String.format("SampleFlags{0x%08X, sync=%s, dependsOn=%d, isDependedOn=%d, leading=%d, redundancy=%d, priority=%d}",
                flags,
                isSyncSample() ? "yes" : "no",
                sampleDependsOn,
                sampleIsDependedOn,
                isLeading,
                sampleHasRedundancy,
                sampleDegradationPriority);
    }

    /**
     * Creates default flags for a sync sample (key frame) that doesn't depend on others.
     *
     * @return SampleFlags for a key frame
     */
    public static SampleFlags createSyncSampleFlags() {
        // sampleDependsOn=2 (does not depend), sampleIsNonSync=0 (is sync/key frame)
        int flags = (2 << 24); // Set sampleDependsOn to 2
        return new SampleFlags(flags);
    }

    /**
     * Creates default flags for a non-sync sample (non-key frame) that depends on others.
     *
     * @return SampleFlags for a non-key frame
     */
    public static SampleFlags createNonSyncSampleFlags() {
        // sampleDependsOn=1 (depends on others), sampleIsNonSync=1 (is non-sync)
        int flags = (1 << 24) | (1 << 16);
        return new SampleFlags(flags);
    }
}
