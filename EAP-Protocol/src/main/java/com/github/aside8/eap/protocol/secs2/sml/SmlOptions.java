package com.github.aside8.eap.protocol.secs2.sml;

/**
 * Configuration/options for the SML parser PoC.
 * All limits are defensive defaults and configurable for CI/production use.
 */
public final class SmlOptions {
    public final int maxListDepth;
    public final int maxElementsPerList;
    public final int maxTotalBytes;

    public static final SmlOptions DEFAULT = new SmlOptions(16, 10_000, 1 * 1024 * 1024);

    public SmlOptions(int maxListDepth, int maxElementsPerList, int maxTotalBytes) {
        if (maxListDepth < 1) throw new IllegalArgumentException("maxListDepth must be >= 1");
        this.maxListDepth = maxListDepth;
        this.maxElementsPerList = Math.max(1, maxElementsPerList);
        this.maxTotalBytes = Math.max(1, maxTotalBytes);
    }
}
