package com.project.shared;

import java.io.PrintStream;

public class ProgressBar {
    
    private int maxProgress;
    private double currentProgress;
    private int barLength;
    private double currentBarProgress;
    private double progressPerBar;
    private PrintStream out;

    public ProgressBar(int maxProgress, int barLength, PrintStream out) {
        this.out = out;
        this.maxProgress = maxProgress;
        this.barLength = barLength;
        this.currentProgress = 0;
        this.currentBarProgress = 0;
        this.progressPerBar = (double) maxProgress / barLength;
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

        while (currentBarProgress >= progressPerBar && currentProgress < maxProgress) {
            currentProgress += progressPerBar;
            currentBarProgress -= progressPerBar;
            write("\u2588");
        }
    }

    private synchronized void write(String s) {
        out.print(s);
    }

    public double getProgress() {
        return currentProgress;
    }
}