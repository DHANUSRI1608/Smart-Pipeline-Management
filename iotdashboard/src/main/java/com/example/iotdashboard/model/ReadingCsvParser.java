package com.example.iotdashboard.model;

import java.time.LocalDateTime;

/**
 * Parses "nodeId,temperature,humidity,pressure,light[,timestamp]"
 * Example: "4,29.1,55.8,1011.5,25.7" or "4,29.1,55.8,1011.5,25.7,2025-08-22T18:30:00"
 */
public final class ReadingCsvParser {

    private ReadingCsvParser() {}

    public static Reading parse(String csv) {
        if (csv == null || csv.isBlank())
            throw new IllegalArgumentException("Empty CSV payload");

        String[] parts = csv.split("\\s*,\\s*");
        if (parts.length < 5)
            throw new IllegalArgumentException("Expected at least 5 fields: nodeId,temperature,humidity,pressure,light");

        Reading r = new Reading();
        r.setTemperature(Double.valueOf(parts[1]));
        r.setHumidity(Double.valueOf(parts[2]));
        r.setPressure(Double.valueOf(parts[3]));
        r.setLight(Double.valueOf(parts[4]));

        if (parts.length >= 6 && !parts[5].isBlank()) {
            r.setTimestamp(LocalDateTime.parse(parts[5]));
        }
        return r;
    }
}