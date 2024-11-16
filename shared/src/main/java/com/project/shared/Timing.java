package com.project.shared;

public class Timing {
    private String name;
    private long time;

    public Timing(String name, long time) {
        this.name = name;
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public long getTime() {
        return time;
    }
}
