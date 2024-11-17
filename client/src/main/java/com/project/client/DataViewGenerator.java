package com.project.client;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.function.Function;

public class DataViewGenerator {

    private final TimestampHandler[][] testResults;
    private final String[] rowLabels;
    private final String[] colLabels;

    /**
     * Represents the format types for data values.
     * <p>
     * The available format types are:
     * - MILLISECONDS: For time values in milliseconds.
     * - PERCENTAGE: For ratios between 0 and 1 (expects a long value calculated as
     * (long) (ratio * 100)).
     * - RAW: For plain numbers without formatting.
     */
    public enum FormatType {
        MILLISECONDS,
        PERCENTAGE,
        DOUBLE_TWO_DECIMAL,
        RAW
    }

    public DataViewGenerator(TimestampHandler[][] testResults) {
        this.testResults = testResults;

        // Define row and column labels based on matrix size and thread count for
        // example
        this.rowLabels = new String[] { "256", "512", "1024", "2048", "4096"};
        this.colLabels = new String[] { "1 Thread", "3 Threads", "7 Threads", "15 Threads", "31 Threads" };
    }

    public String[][] generateView(Function<TimestampHandler, Long> metricFunc, FormatType formatType) {
        String[][] view = new String[rowLabels.length][colLabels.length];

        DecimalFormat percentageFormat = new DecimalFormat("0.00%");
        DecimalFormat msFormat = new DecimalFormat("#,### ms");
        DecimalFormat twoDecimalFormat = new DecimalFormat("#.##");

        // Populate view with metrics from each TimestampHandler, handling nulls
        for (int i = 0; i < rowLabels.length; i++) {
            for (int j = 0; j < colLabels.length; j++) {
                TimestampHandler handler = testResults[i][j];
                if (handler != null) {
                    long value = metricFunc.apply(handler);
                    switch (formatType) {
                        case MILLISECONDS:
                            view[i][j] = msFormat.format(value);
                            break;
                        case PERCENTAGE:
                            // interpret val as integer-based percentage
                            view[i][j] = percentageFormat.format(value / 100.0);
                            break;
                        case DOUBLE_TWO_DECIMAL:
                            view[i][j] = twoDecimalFormat.format(value / 100.0);
                            break;
                        default:
                            view[i][j] = String.valueOf(value); // RAW format
                    }
                } else {
                    view[i][j] = "-"; // Placeholder for missing data
                }
            }
        }

        return view;
    }

    // Methods to generate views for specific metrics
    public String[][] generateTotalTimeView() {
        return generateView(TimestampHandler::getTotalTime, FormatType.MILLISECONDS);
    }

    public String[][] generateProcessingTimeView() {
        return generateView(TimestampHandler::getTotalProcessingTime, FormatType.MILLISECONDS);
    }

    /**
     * Generates a view of networking time data.
     *
     * @return a two-dimensional array containing the generated view
     */
    public String[][] generateNetworkingTimeView() {
        return generateView(TimestampHandler::getTotalNetworkingTime, FormatType.MILLISECONDS);
    }

    /**
     * Generates a data view for networking processing overlap ratio.
     * This method calculates the overlap ratio using a handler and returns the result as a 2D string array.
     * The overlap ratio is converted to a percentage and formatted accordingly.
     *
     * @return a 2D string array representing the networking processing overlap ratio view.
     */
    public String[][] generateNetworkingProcessingOverlapRatioView() {
        return generateView(handler -> {
            double overlapRatio = handler.getNetworkingProcessingOverlapRatio();
            return (long) (overlapRatio * 100); // Convert to percentage (optional formatting)
        }, FormatType.PERCENTAGE);
    }

    /**
     * Generates a speedup view for a given metric.
     * 
     * The speedup is calculated as the ratio of the baseline time to the current
     *
     * @param metricName the name of the metric to generate the speedup view for
     * @return a 2-dimensional array representing the speedup view
     */
    public String[][] generateSpeedupView(String metricName) {
        return generateView(handler -> {
            int row = findRowIndex(handler);
            if (row == -1 || testResults[row][0] == null) {
                return null; // No baseline for 1 thread
            }
            long baselineTime = testResults[row][0].getMetric(metricName);
            long currentTime = handler.getMetric(metricName);
            double ratio = (double) baselineTime / currentTime;
            return (long) (ratio * 100); // Speedup calculation
        }, FormatType.DOUBLE_TWO_DECIMAL);
    }

    /**
     * Generates an efficiency view for a given metric.
     * 
     * The efficiency is calculated as the ratio of the speedup to the number of threads.
     *
     * @param metricName the name of the metric to generate the view for
     * @return a 2D array representing the efficiency view
     */
    public String[][] generateEfficiencyView(String metricName) {
        return generateView(handler -> {
            int row = findRowIndex(handler);
            int col = findColIndex(handler);
            if (row == -1 || col == -1 || testResults[row][0] == null) {
                return null; // No baseline for 1 thread
            }
            long baselineTime = testResults[row][0].getMetric(metricName);
            long currentTime = handler.getMetric(metricName);
            double speedup = (double) baselineTime / currentTime;
            int threads = getThreadCountForColumn(col);
            double ratio = speedup / threads;
            return (long) (ratio * 100); // Efficiency calculation
        }, FormatType.PERCENTAGE);
    }

    private int findRowIndex(TimestampHandler handler) {
        for (int i = 0; i < testResults.length; i++) {
            if (Arrays.asList(testResults[i]).contains(handler))
                return i;
        }
        return -1;
    }

    private int findColIndex(TimestampHandler handler) {
        for (int i = 0; i < testResults[0].length; i++) {
            for (int j = 0; j < testResults.length; j++) {
                if (testResults[j][i] == handler)
                    return i;
            }
        }
        return -1;
    }

    private int getThreadCountForColumn(int colIndex) {
        if (colIndex == 1) {
            return 3;
        } else if (colIndex == 2) {
            return 7;
        } else if (colIndex == 3) {
            return 15;
        } else if (colIndex == 4) {
            return 31;
        } else {
            return 1;
        }
    }
}
