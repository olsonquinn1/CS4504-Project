package com.project.shared;

import java.io.Serializable;

public class ProfilingData implements Serializable {
    private final int coreCount;
    private final double speedRating;

    public ProfilingData(int coreCount, double speedRating) {
        this.coreCount = coreCount;
        this.speedRating = speedRating;
    }

    public int getCoreCount() {
        return coreCount;
    }

    public double getSpeedRating() {
        return speedRating;
    }
}