package org.drom.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * EVENT - A Data Model (also called a "POJO" - Plain Old Java Object)
 * 
 * This class represents what an "Event" looks like in our application.
 * Think of it as a blueprint or template for event data.
 * 
 * When we get data from the database or from a user's request,
 * we store it in an Event object so it's easy to work with.
 * 
 * LOMBOK ANNOTATIONS (these save us from writing boring code):
 * 
 * @Data - Automatically creates:
 *         - Getters: getName(), getLocation(), etc.
 *         - Setters: setName("Concert"), setLocation("Park"), etc.
 *         - toString(): for printing the object
 *         - equals() and hashCode(): for comparing objects
 * 
 * @NoArgsConstructor - Creates an empty constructor: new Event()
 * 
 * @AllArgsConstructor - Creates a constructor with ALL fields:
 *                       new Event(id, name, description, dateTime, location, zipCode, category, sourceUrl)
 * 
 * Without Lombok, we'd have to write 100+ lines of boilerplate code!
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    
    // ============ FIELDS (the data an Event holds) ============
    
    /**
     * Unique identifier - like a social security number for events.
     * We use UUID (Universally Unique ID) to ensure no two events have the same ID.
     * Example: "550e8400-e29b-41d4-a716-446655440000"
     */
    private String id;
    
    /**
     * The name/title of the event.
     * Example: "Summer Music Festival"
     */
    private String name;
    
    /**
     * A short description of what the event is about.
     * Example: "Join us for live music, food trucks, and fun!"
     */
    private String description;
    
    /**
     * When the event takes place.
     * LocalDateTime stores both date AND time.
     * Example: 2025-07-15T19:00:00 (July 15, 2025 at 7:00 PM)
     */
    private LocalDateTime dateTime;
    
    /**
     * Where the event is happening.
     * Example: "Central Park, Main Stage"
     */
    private String location;
    
    /**
     * The 5-digit zip code for the event location.
     * This is our PRIMARY KEY in DynamoDB - we use it to search for events!
     * Example: "84098"
     */
    private String zipCode;
    
    /**
     * What type of event this is.
     * Example: "Concert", "Festival", "Market", "Sports"
     */
    private String category;
    
    /**
     * A URL link to more information or to buy tickets.
     * Example: "https://ticketmaster.com/event/12345"
     */
    private String sourceUrl;
    
    // ============ CUSTOM CONSTRUCTOR ============
    
    /**
     * A simpler constructor for when we don't have all the fields.
     * This is useful when reading from the database where we only store some fields.
     * 
     * @param id       The unique event ID
     * @param name     Event name
     * @param location Where it's happening
     * @param zipCode  The zip code (for searching)
     * @param date     Date as a String like "2025-07-15T19:00:00"
     */
    public Event(String id, String name, String location, String zipCode, String date) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.zipCode = zipCode;
        this.dateTime = parseDate(date);  // Convert String to LocalDateTime
    }

    // ============ HELPER METHOD ============
    
    /**
     * Converts a date String into a LocalDateTime object.
     * 
     * Why? Databases and JSON often send dates as text (Strings).
     * But in Java, it's easier to work with LocalDateTime objects
     * because we can do things like: dateTime.plusDays(7)
     * 
     * @param date A date string in ISO format: "2025-07-15T19:00:00"
     * @return A LocalDateTime object we can work with in Java
     */
    private LocalDateTime parseDate(String date) {
        // ISO_LOCAL_DATE_TIME expects format: "2025-09-14T18:30:00"
        // The 'T' separates the date from the time
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return LocalDateTime.parse(date, formatter);
    }

}