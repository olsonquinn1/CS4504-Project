package com.project.shared;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

/**
 * BufferedLogHandler is a utility class that writes log messages to a JavaFX TextArea.
 * 
 * This class is thread-safe and can be used to log messages from any thread.
 * 
 * Usage:
 * 
 * TextArea textArea = new TextArea();
 * BufferedLogHandler logHandler = new BufferedLogHandler(textArea, 1000);
 * PrintStream log = logHandler.getLogStream();
 * 
 * log.println("Hello, world!");
 * 
 * // When the application closes, stop the log handler
 * logHandler.stop();
 */
public class BufferedLogHandler {
    
    private final TextArea textArea;
    private final List<String> logBuffer = new ArrayList<>();
    private final Object logLock = new Object();
    private final Timer logTimer = new Timer(true);
    private final PrintStream log;

    /**
     * Creates a BufferedLogHandler that writes log entries to a JavaFX TextArea
     * 
     * Safe to use from any thread.
     * 
     * @param textArea The TextArea where log entries will be displayed.
     * @param flushIntervalMs Interval in milliseconds to flush the buffer to the TextArea.
     */
    public BufferedLogHandler(TextArea textArea, long flushIntervalMs) {
        this.textArea = textArea;

        // Create a PrintStream that writes to the buffer
        OutputStream outStream = new OutputStream() {
            @Override
            public void write(int b) {
                bufferLog(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) {
                bufferLog(new String(b, off, len));
            }
        };

        this.log = new PrintStream(outStream, true);

        // Schedule periodic flushing of the log buffer to the TextArea
        logTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                flushLogBuffer();
            }
        }, 0, flushIntervalMs);
    }

    /**
     * Returns the PrintStream that writes to this log handler.
     * Use this PrintStream to log messages.
     */
    public PrintStream getLogStream() {
        return log;
    }

    /**
     * Adds a log message to the buffer.
     */
    private void bufferLog(String message) {
        synchronized (logLock) {
            logBuffer.add(message);
        }
    }

    /**
     * Flushes the buffer to the TextArea on the JavaFX Application Thread.
     */
    private void flushLogBuffer() {
        List<String> messagesToLog;
        synchronized (logLock) {
            if (logBuffer.isEmpty()) {
                return;
            }
            messagesToLog = new ArrayList<>(logBuffer);
            logBuffer.clear();
        }

        Platform.runLater(() -> {
            for (String message : messagesToLog) {
                textArea.appendText(message);
            }
        });
    }

    /**
     * Stops the timer and clears resources when the application closes.
     */
    public void stop() {
        logTimer.cancel();
    }
}