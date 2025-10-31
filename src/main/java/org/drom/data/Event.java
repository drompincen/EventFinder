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
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
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
        return LocalDateTime.parse(date, DATE_FORMATTER);
    }

}
