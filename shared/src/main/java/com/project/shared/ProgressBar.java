package com.project.shared;

public class ProgressBar {
    
    private int maxProgress;
    private int currentProgress;
    private int barLength;
    private int currentBarProgress;
    private double progressPerBar;

    public ProgressBar(int maxProgress, int barLength) {
        this.maxProgress = maxProgress;
        this.barLength = barLength;
        this.currentProgress = 0;
        this.progressPerBar = (double) maxProgress / barLength;
    }

    public void start() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            sb.append("-");
        }
        sb.append("\n");

        write(sb.toString());
    }

    public void reset() {
        write("progress: " + currentProgress + "/" + maxProgress + "\n");
        currentProgress = 0;
        currentBarProgress = 0;
        
        write("\n");
    }

    public synchronized void progress(int amount) {
        currentBarProgress += amount;
        while (currentBarProgress >= progressPerBar && currentProgress < maxProgress) {
            currentProgress += amount;
            write("\u2588");
            currentBarProgress -= progressPerBar;
        }
    }

    private synchronized void write(String s) {
        System.out.print(s);
    }

    public void printCurrentProgress() {
        write("progress: " + currentProgress + "/" + maxProgress + "\n");
    }
}
