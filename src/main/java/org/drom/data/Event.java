package org.drom.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private String id;                 // Unique identifier (UUID or hash)
    private String name;               // Event title
    private String description;        // Short summary
    private LocalDateTime dateTime;    // When it's happening
    private String location;           // Venue or address
    private String zipCode;            // For filtering
    private String category;           // e.g., Concert, Market, Activity
    private String sourceUrl;          // Link to more info or tickets
    public Event(String id, String name, String location, String zipCode, String date) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.zipCode = zipCode;
        this.dateTime = parseDate(date);
    }

    private LocalDateTime parseDate(String date) {
        // Adjust format to match your input, e.g. "2025-09-14T18:30"
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return LocalDateTime.parse(date, formatter);
    }

}