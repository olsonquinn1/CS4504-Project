package com.project.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.project.shared.Timestamp;

/*
Timestamps
"task sent by client"
"task received by router"
"task divided by router"
"M# sent to server" (7)
"M# received by server" (7)
"M# sent by server" (7)
"M# received by router" (7)
"result sent by router"
"result received by client"
 */

/**
 * The `TimestampHandler` class is responsible for processing a list of timestamps and calculating various metrics based on those timestamps.
 * It provides methods to retrieve the calculated metrics and timestamps.
 */
public class TimestampHandler {

    private final List<Timestamp> timestamps;
    private final Map<String, Long> timestampMap;
    private final Map<String, Long> metricMap;

    private double networkingProcessingOverlapRatio;

    /**
     * Constructs a TimestampHandler object with the given list of timestamps.
     *
     * @param timestamps the list of timestamps to be processed
     * @throws IllegalArgumentException if any timestamps are missing
     */
    public TimestampHandler(List<Timestamp> timestamps) throws IllegalArgumentException {

        this.timestamps = timestamps;
        this.timestampMap = new HashMap<>();
        this.metricMap = new HashMap<>();

        for (Timestamp timestamp : timestamps) {
            timestampMap.put(timestamp.getLabel(), timestamp.getTimestamp());
        }

        List<String> missingTimestamps = validateTimestamps();
        if (!missingTimestamps.isEmpty()) {
            List<String> haveTimestamps = new ArrayList<>(timestampMap.keySet());
            throw new IllegalArgumentException(
                    "Missing timestamps: " + missingTimestamps
                            + "\nTimestamps: " + haveTimestamps);
        }

        processTimestamps();
    }

    private void processTimestamps() {
        metricMap.put("total time", calculateTotalTime()); // total time
        metricMap.put("server processing time", calculateServerProcessingTime()); // server parallel processing time
        metricMap.put("router dividing time", calculateRouterDividingTime()); // router dividing time
        metricMap.put("total processing time", // total processing time
                metricMap.get("server processing time") + metricMap.get("router dividing time"));
        metricMap.put("total networking time", calculateTotalNetworkingTime()); // total networking time

        networkingProcessingOverlapRatio = calculateNetworkingProcessingOverlapRatio();
    }

    /**
     * Validates the timestamps in the timestampMap.
     * 
     * @return a list of missing timestamps.
     */
    private List<String> validateTimestamps() {

        // boolean good = timestampMap.containsKey("task sent by client")
        // && timestampMap.containsKey("task received by router")
        // && timestampMap.containsKey("task divided by router")
        // && timestampMap.containsKey("result sent by router")
        // && timestampMap.containsKey("result received by client");

        // for (int i = 1; i <= 7; i++) {
        // good &= timestampMap.containsKey("M" + i + " sent to server")
        // && timestampMap.containsKey("M" + i + " received by server")
        // && timestampMap.containsKey("M" + i + " sent by server")
        // && timestampMap.containsKey("M" + i + " received by router");
        // }

        // return good;

        List<String> missingTimestamps = new ArrayList<>();
        if (!timestampMap.containsKey("task sent by client")) {
            missingTimestamps.add("task sent by client");
        }
        if (!timestampMap.containsKey("task received by router")) {
            missingTimestamps.add("task received by router");
        }
        if (!timestampMap.containsKey("task divided by router")) {
            missingTimestamps.add("task divided by router");
        }
        if (!timestampMap.containsKey("result sent by router")) {
            missingTimestamps.add("result sent by router");
        }
        if (!timestampMap.containsKey("result received by client")) {
            missingTimestamps.add("result received by client");
        }

        for (int i = 1; i <= 7; i++) {
            if (!timestampMap.containsKey("M" + i + " sent to server")) {
                missingTimestamps.add("M" + i + " sent to server");
            }
            if (!timestampMap.containsKey("M" + i + " received by server")) {
                missingTimestamps.add("M" + i + " received by server");
            }
            if (!timestampMap.containsKey("M" + i + " sent by server")) {
                missingTimestamps.add("M" + i + " sent by server");
            }
            if (!timestampMap.containsKey("M" + i + " received by router")) {
                missingTimestamps.add("M" + i + " received by router");
            }
        }

        return missingTimestamps;
    }

    public long getTimestamp(String label) {
        return timestampMap.get(label);
    }

    public long getMetric(String label) {
        return metricMap.get(label);
    }

    public long getTotalTime() {
        return getMetric("total time");
    }

    public long getTotalProcessingTime() {
        return getMetric("total processing time");
    }

    public long getTotalNetworkingTime() {
        return getMetric("total networking time");
    }

    public double getNetworkingProcessingOverlapRatio() {
        return networkingProcessingOverlapRatio;
    }


    /**
     * Calculates the total time taken for a task by subtracting the start time from the end time.
     *
     * @return The total time taken for the task in milliseconds.
     */
    private long calculateTotalTime() {
        long startTime = getTimestamp("task sent by client");
        long endTime = getTimestamp("result received by client");
        return endTime - startTime;
    }

    /**
     * Calculates the non-overlapping processing time of the servers.
     *
     * @return The processing time of the server in milliseconds.
     */
    private long calculateServerProcessingTime() {
        List<long[]> processing_intervals = getServerIntervals("received by server", "sent by server");
        return calculateNonOverlappingTime(processing_intervals);
    }

    /**
     * Calculates the time taken for a task to be divided by the router.
     *
     * @return the time taken for the task to be divided by the router in milliseconds.
     */
    private long calculateRouterDividingTime() {
        return getTimestamp("task divided by router") - getTimestamp("task received by router");
    }

    /**
     * Calculates the total networking time by calculating the non-overlapping time intervals
     * between all network interactions.
     *
     * @return The total networking time in milliseconds.
     */
    private long calculateTotalNetworkingTime() {

        List<long[]> intervals = new ArrayList<>();

        intervals.add(
                new long[] {
                        getTimestamp("task sent by client"), getTimestamp("task received by router")
                }); // client -> router

        List<long[]> server_recv_intervals = getServerIntervals("sent to server", "received by server");
        intervals.addAll(server_recv_intervals); // router -> server

        List<long[]> server_send_intervals = getServerIntervals("sent by server", "received by router");
        intervals.addAll(server_send_intervals); // server -> router

        intervals.add(
                new long[] {
                        getTimestamp("result sent by router"), getTimestamp("result received by client")
                }); // router -> client

        return calculateNonOverlappingTime(intervals);
    }

    /**
     * Calculates the ratio of networking time overlapping with processing time
     * within the specified interval.
     *
     * @return the ratio of overlapping networking time to the total interval time.
     *         (higher means more overlap = good)
     */
    private double calculateNetworkingProcessingOverlapRatio() {
        long intervalStart = getTimestamp("task received by router");
        long intervalEnd = getTimestamp("result sent by router");
        long totalIntervalTime = intervalEnd - intervalStart;

        // Get networking intervals within the specified time frame
        List<long[]> networkingIntervals = new ArrayList<>();
        networkingIntervals
                .add(new long[] { getTimestamp("task sent by client"), getTimestamp("task received by router") });
        networkingIntervals
                .add(new long[] { getTimestamp("result sent by router"), getTimestamp("result received by client") });
        networkingIntervals.addAll(getServerIntervals("sent to server", "received by server"));
        networkingIntervals.addAll(getServerIntervals("sent by server", "received by router"));

        // merge networking intervals
        networkingIntervals = mergeIntervals(networkingIntervals);

        // Get processing intervals within the specified time frame
        List<long[]> processingIntervals = getServerIntervals("received by server", "sent by server");

        // Filter intervals to keep only those within the main interval
        networkingIntervals = filterIntervalsWithinRange(networkingIntervals, intervalStart, intervalEnd);
        processingIntervals = filterIntervalsWithinRange(processingIntervals, intervalStart, intervalEnd);

        // Calculate the overlapping time between networking and processing intervals
        long overlappingTime = calculateOverlapBetweenIntervals(networkingIntervals, processingIntervals, intervalStart,
                intervalEnd);

        // Avoid division by zero
        if (totalIntervalTime == 0) {
            return 0.0;
        }

        // Calculate and return the ratio
        return (double) overlappingTime / totalIntervalTime;
    }

    /**
     * Calculates the total non-overlapping time from a list of intervals.
     *
     * @param intervals a list of intervals represented as long arrays, where each
     *                  array contains two elements: the start time and end time of
     *                  the interval.
     * @return the total non-overlapping time in milliseconds.
     */
    private long calculateNonOverlappingTime(List<long[]> intervals) {
        List<long[]> mergedIntervals = mergeIntervals(intervals);
        return mergedIntervals.stream()
                .mapToLong(interval -> interval[1] - interval[0])
                .sum();
    }

    /**
     * Calculates the non-overlapping server time between two labels.
     *
     * @param labelA the first label
     * @param labelB the second label
     * @return the non-overlapping server time
     */
    private List<long[]> getServerIntervals(String labelA, String labelB) {

        // Collect pairs of "M# <labelA>" -> "M# <labelB>"
        List<long[]> intervals = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            long start = getTimestamp("M" + i + " " + labelA);
            long end = getTimestamp("M" + i + " " + labelB);
            if (start != -1 && end != -1) {
                intervals.add(new long[] { start, end });
            }
        }

        return intervals;
    }

    /**
     * Merges overlapping intervals and returns the resulting list of merged
     * intervals.
     * 
     * @param intervals the original list of intervals
     * @return a new list of merged intervals
     */
    public List<long[]> mergeIntervals(List<long[]> intervals) {
        if (intervals.isEmpty())
            return Collections.emptyList();

        // Sort intervals by their start time
        intervals.sort(Comparator.comparingLong(a -> a[0]));

        List<long[]> mergedIntervals = new ArrayList<>();
        long[] currentInterval = intervals.get(0);

        for (int i = 1; i < intervals.size(); i++) {
            long[] nextInterval = intervals.get(i);

            if (currentInterval[1] >= nextInterval[0]) {
                // Overlapping intervals, merge them
                currentInterval[1] = Math.max(currentInterval[1], nextInterval[1]);
            } else {
                // No overlap, add the current interval to the list
                mergedIntervals.add(currentInterval);
                currentInterval = nextInterval;
            }
        }

        // Add the last interval
        mergedIntervals.add(currentInterval);

        return mergedIntervals;
    }

    /**
     * Filters intervals to keep only those within the specified range.
     *
     * @param intervals the list of intervals to filter.
     * @param start     the start of the range.
     * @param end       the end of the range.
     * @return a list of intervals that fall within the specified range.
     */
    private List<long[]> filterIntervalsWithinRange(List<long[]> intervals, long start, long end) {
        List<long[]> filteredIntervals = new ArrayList<>();
        for (long[] interval : intervals) {
            long intervalStart = Math.max(interval[0], start);
            long intervalEnd = Math.min(interval[1], end);
            if (intervalStart < intervalEnd) { // Only add intervals with valid duration
                filteredIntervals.add(new long[] { intervalStart, intervalEnd });
            }
        }
        return filteredIntervals;
    }

    /**
     * Calculates the total overlapping time between two lists of intervals,
     * ensuring overlaps stay within the specified interval bounds.
     *
     * @param intervalsA    the first list of intervals (e.g., networking
     *                      intervals).
     * @param intervalsB    the second list of intervals (e.g., processing
     *                      intervals).
     * @param intervalStart the start of the main interval.
     * @param intervalEnd   the end of the main interval.
     * @return the total overlapping time in milliseconds within the main interval
     *         bounds.
     */
    private long calculateOverlapBetweenIntervals(List<long[]> intervalsA, List<long[]> intervalsB, long intervalStart,
            long intervalEnd) {
        long overlappingTime = 0;

        for (long[] intervalA : intervalsA) {
            for (long[] intervalB : intervalsB) {
                // Calculate the overlap within the specified bounds
                long overlapStart = Math.max(intervalA[0], Math.max(intervalB[0], intervalStart));
                long overlapEnd = Math.min(intervalA[1], Math.min(intervalB[1], intervalEnd));

                if (overlapStart < overlapEnd) {
                    overlappingTime += overlapEnd - overlapStart;
                }
            }
        }

        return overlappingTime;
    }

}
