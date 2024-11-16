package com.project.shared;

import java.io.PrintStream;

public class ProgressBar {
    
    private int maxProgress;
    private double currentProgress;
    private int barLength;
    private double currentBarProgress;
    private double progressPerBar;
    private PrintStream out;

    private long updatesPerSecond = 2;
    private long lastUpdateTime = 0;
    private long updateInterval;

    public ProgressBar(int maxProgress, int barLength, PrintStream out) {
        this.out = out;
        this.maxProgress = maxProgress;
        this.barLength = barLength;
        this.currentProgress = 0;
        this.currentBarProgress = 0;
        this.progressPerBar = (double) maxProgress / barLength;

        this.updateInterval = 1000 / updatesPerSecond;
    }

    public void setUpdatesPerSecond(long updatesPerSecond) {
        this.updatesPerSecond = updatesPerSecond;
        this.updateInterval = 1000 / updatesPerSecond;
    }

    public void start() {
        reset();
    }

    public void stop() {
        write("\n");
    }

    private void writeIndicatorBar() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            sb.append("-");
        }
        sb.append("\n");

        write(sb.toString());
    }

    public void reset() {
        currentProgress = 0;
        currentBarProgress = 0;
        writeIndicatorBar();
    }

    public synchronized void progress(int amount) {
        currentBarProgress += amount;

        if(currentBarProgress >= maxProgress) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        if(!(currentProgress + currentBarProgress >= maxProgress) && currentTime - lastUpdateTime < updateInterval) {
            return;
        }

        while (currentBarProgress >= progressPerBar && currentProgress < maxProgress) {
            currentProgress += progressPerBar;
            currentBarProgress -= progressPerBar;
            write("\u2588");
        }

        lastUpdateTime = currentTime;
    }

    private synchronized void write(String s) {
        out.print(s);
    }

    public double getProgress() {
        return currentProgress;
    }
}