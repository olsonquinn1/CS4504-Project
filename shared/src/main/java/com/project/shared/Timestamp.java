package com.project.shared;

import java.io.Serializable;

public class Timestamp implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long timestamp;

    private final String label;

    public Timestamp(String label) {
        this.label = label;
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getLabel() {
        return label;
    }
}
