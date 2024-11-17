package com.project.shared;

import java.io.Serializable;

/**
 * The ProfilingData class represents profiling data for a system.
 * It contains information about the core count and speed rating of the system.
 */
public class ProfilingData implements Serializable {
    private final int coreCount;
    private final double speedRating;

    /**
     * Constructs a ProfilingData object with the given core count and speed rating.
     *
     * @param coreCount   the number of cores in the system
     * @param speedRating the speed rating of the system
     */
    public ProfilingData(int coreCount, double speedRating) {
        this.coreCount = coreCount;
        this.speedRating = speedRating;
    }

    /**
     * Returns the core count of the system.
     *
     * @return the core count
     */
    public int getCoreCount() {
        return coreCount;
    }

    /**
     * Returns the speed rating of the system.
     *
     * @return the speed rating
     */
    public double getSpeedRating() {
        return speedRating;
    }
}