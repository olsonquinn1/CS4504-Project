package com.project.shared;

import java.io.PrintStream;

/**
 * The ProgressBar class represents a progress bar that can be used to display the progress of a task.
 */
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

    /**
     * Constructs a ProgressBar object with the specified maximum progress, bar length, and output stream.
     * 
     * @param maxProgress the maximum progress value
     * @param barLength the length of the progress bar
     * @param out the output stream to write the progress bar to
     */
    public ProgressBar(int maxProgress, int barLength, PrintStream out) {
        this.out = out;
        this.maxProgress = maxProgress;
        this.barLength = barLength;
        this.currentProgress = 0;
        this.currentBarProgress = 0;
        this.progressPerBar = (double) maxProgress / barLength;

        this.updateInterval = 1000 / updatesPerSecond;
    }

    /**
     * Sets the number of updates per second for the progress bar.
     * 
     * @param updatesPerSecond the number of updates per second
     */
    public void setUpdatesPerSecond(long updatesPerSecond) {
        this.updatesPerSecond = updatesPerSecond;
        this.updateInterval = 1000 / updatesPerSecond;
    }

    /**
     * Starts the progress bar.
     */
    public void start() {
        reset();
    }

    /**
     * Stops the progress bar and writes a new line character.
     */
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

    /**
     * Resets the progress bar to its initial state.
     */
    public synchronized void reset() {
        currentProgress = 0;
        currentBarProgress = 0;
        writeIndicatorBar();
    }

    /**
     * Updates the progress of the progress bar by the specified amount.
     * 
     * @param amount the amount to progress the bar by
     */
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

    /**
     * Returns the current progress of the progress bar.
     * 
     * @return the current progress
     */
    public double getProgress() {
        return currentProgress;
    }
}