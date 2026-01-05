package org.drom.service;

import org.drom.data.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * EVENT FINDER SERVICE - The "Brain" of Our Application
 *
 * This is where the BUSINESS LOGIC lives. While the Controller is like
 * a receptionist (receives requests), the Service is like the actual
 * worker who does the job.
 *
 * WHAT IS A SERVICE?
 * In the "layered architecture" pattern, we separate our code into layers:
 *
 *   ┌─────────────────┐
 *   │   Controller    │  ← Handles HTTP requests/responses
 *   └────────┬────────┘
 *            │
 *   ┌────────▼────────┐
 *   │    Service      │  ← Business logic (YOU ARE HERE!)
 *   └────────┬────────┘
 *            │
 *   ┌────────▼────────┐
 *   │   Repository    │  ← Database operations (we use DynamoDB directly)
 *   └─────────────────┘
 *
 * WHY SEPARATE LAYERS?
 *   - Easier to test (we can test each layer independently)
 *   - Easier to maintain (changes in one layer don't break others)
 *   - Easier to understand (each class has ONE job)
 *
 * @Service - Tells Spring: "This is a service class. Create one instance
 *            and make it available for dependency injection."
 */
@Service
public class EventFinder {

    /**
     * Our connection to DynamoDB.
     * We use this to read and write data to the database.
     *
     * 'final' means this can't be changed after it's set.
     * This is a good practice - our database connection shouldn't change!
     */
    private final DynamoDbClient dynamoDbClient;

    /**
     * Constructor - Spring automatically provides the DynamoDbClient.
     *
     * @Autowired tells Spring: "I need a DynamoDbClient to work!"
     * Spring looks for a @Bean that returns DynamoDbClient (in DynamoDbConfig)
     * and automatically passes it here. Magic! ✨
     *
     * @param dynamoDbClient The database client (injected by Spring)
     */
    @Autowired
    public EventFinder(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    // ═══════════════════════════════════════════════════════════════════
    //                         READ OPERATIONS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * FIND EVENTS BY ZIP CODE
     *
     * This method searches the database for all events in a given zip code.
     * If no events are found, it returns some mock/generated events instead.
     *
     * HOW DYNAMODB QUERIES WORK:
     *
     * DynamoDB is a "key-value" database. Our table has:
     *   - Partition Key: zipCode (like a folder name)
     *   - Sort Key: id (like a file name within the folder)
     *
     * When we query by zipCode, DynamoDB quickly finds all items
     * in that "folder". This is SUPER fast, even with millions of records!
     *
     * @param zipCode The 5-digit zip code to search for (e.g., "84098")
     * @return A list of events in that zip code, or mock events if none found
     */
    public List<Event> findEventsByZipCode(String zipCode) {
        // The name of our DynamoDB table
        String tableName = "Events";

        /*
         * EXPRESSION ATTRIBUTE VALUES
         *
         * In DynamoDB, we use "placeholders" in our queries for safety.
         * Instead of: "zipCode = 84098"
         * We write:   "zipCode = :zip"  and then say :zip means "84098"
         *
         * Why? It prevents "injection attacks" where bad users try to
         * hack your database by putting code in the zip code field.
         *
         * AttributeValue.builder().s(zipCode) creates a String attribute.
         * The .s() means "string type". DynamoDB also has .n() for numbers.
         */
        Map<String, AttributeValue> expressionValues = Map.of(
                ":zip", AttributeValue.builder().s(zipCode).build()
        );

        /*
         * BUILD THE QUERY REQUEST
         *
         * This is like writing a question for the database:
         * "Hey DynamoDB, in the 'Events' table, find all items
         *  where zipCode equals this value."
         */
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("zipCode = :zip")  // The "WHERE" clause
                .expressionAttributeValues(expressionValues)  // The actual values
                .build();

        try {
            // Execute the query - this talks to DynamoDB!
            QueryResponse response = dynamoDbClient.query(queryRequest);

            /*
             * JAVA STREAMS - A Modern Way to Process Lists
             *
             * response.items() returns a List of database rows.
             * Each row is a Map<String, AttributeValue>.
             *
             * .stream() - Convert the list to a "stream" for processing
             * .map()    - Transform each item into an Event object
             * .collect() - Gather all the results back into a List
             *
             * This is like a factory assembly line:
             * Raw Data → Transform → Package → Ship
             */
            List<Event> events = response.items().stream()
                    .map(item -> new Event(
                            item.get("id").s(),        // Get the "id" field as String
                            item.get("name").s(),      // Get the "name" field
                            item.get("location").s(),  // Get the "location" field
                            item.get("zipCode").s(),   // Get the "zipCode" field
                            item.get("date").s()       // Get the "date" field
                    ))
                    .collect(Collectors.toList());

            // If we found events, return them. Otherwise, generate mock events.
            // This is a "ternary operator" - a compact if/else statement
            return events.isEmpty() ? generateEventsWithAI(zipCode) : events;

        } catch (DynamoDbException e) {
            // Something went wrong with the database!
            // Log the error and return mock events as a fallback
            System.err.println("DynamoDB query failed: " + e.getMessage());
            return generateEventsWithAI(zipCode);
        }
    }

    /**
     * GENERATE MOCK EVENTS (Fallback Method)
     *
     * When we can't find real events in the database, we return these
     * fake/mock events so the user sees SOMETHING instead of an empty list.
     *
     * In a real app, this could call an AI service to generate relevant
     * events, or scrape event websites, or use a third-party API.
     *
     * UUID.randomUUID() generates a unique ID like:
     * "550e8400-e29b-41d4-a716-446655440000"
     *
     * LocalDateTime.now().plusDays(3) means "3 days from now"
     *
     * @param zipCode The zip code to associate with the mock events
     * @return A list of 3 fake events for demo purposes
     */
    private List<Event> generateEventsWithAI(String zipCode) {
        // List.of() creates an immutable (unchangeable) list
        return List.of(
                new Event(
                        UUID.randomUUID().toString(),     // Random unique ID
                        "Zip Jam Festival",               // Event name
                        "Main Street Park",               // Location
                        zipCode,                          // The requested zip code
                        LocalDateTime.now().plusDays(3).toString()  // 3 days from now
                ),
                new Event(
                        UUID.randomUUID().toString(),
                        "Art & Wine Walk",
                        "Historic District",
                        zipCode,
                        LocalDateTime.now().plusDays(7).toString()  // 1 week from now
                ),
                new Event(
                        UUID.randomUUID().toString(),
                        "Tech Meetup",
                        "Innovation Hub",
                        zipCode,
                        LocalDateTime.now().plusDays(10).toString() // 10 days from now
                )
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    //                         WRITE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * SAVE AN EVENT TO THE DATABASE
     *
     * This method takes an Event object and stores it in DynamoDB.
     * If an event with the same zipCode + id already exists, it will
     * be OVERWRITTEN (this is how DynamoDB's putItem works).
     *
     * HOW DYNAMODB STORES DATA:
     *
     * DynamoDB stores data as "items" (like rows in a spreadsheet).
     * Each item is a collection of "attributes" (like columns).
     *
     * Our Event becomes:
     * {
     *   "zipCode": "84098",        ← Partition Key
     *   "id": "abc-123-def",       ← Sort Key
     *   "name": "Summer Concert",
     *   "location": "Central Park",
     *   "date": "2025-07-15T19:00"
     * }
     *
     * @param event The Event object to save
     */
    public void saveEvent(Event event) {
        /*
         * Convert our Event object into a DynamoDB "item"
         *
         * Map.of() creates an immutable map (like a dictionary).
         * Each entry is: "fieldName" → AttributeValue
         *
         * AttributeValue.builder().s(...) creates a String attribute
         * The .s() stands for "string". DynamoDB also supports:
         *   .n() for numbers
         *   .b() for binary data
         *   .ss() for string sets
         *   .l() for lists
         *   .m() for nested maps
         */
        Map<String, AttributeValue> item = Map.of(
                "id", AttributeValue.builder().s(event.getId()).build(),
                "name", AttributeValue.builder().s(event.getName()).build(),
                "location", AttributeValue.builder().s(event.getLocation()).build(),
                "zipCode", AttributeValue.builder().s(event.getZipCode()).build(),
                "date", AttributeValue.builder().s(String.valueOf(event.getDateTime())).build()
        );

        // Build the request to put (save) the item
        PutItemRequest request = PutItemRequest.builder()
                .tableName("Events")  // Which table to save to
                .item(item)           // The data to save
                .build();

        // Execute the request - this saves to DynamoDB!
        dynamoDbClient.putItem(request);
    }

    /*
     * ═══════════════════════════════════════════════════════════════════
     * CHALLENGES: Add more methods!
     * ═══════════════════════════════════════════════════════════════════
     *
     * 1. DELETE an event:
     *    public void deleteEvent(String zipCode, String id) { ... }
     *    Hint: Use DeleteItemRequest and dynamoDbClient.deleteItem()
     *
     * 2. GET a single event:
     *    public Event getEvent(String zipCode, String id) { ... }
     *    Hint: Use GetItemRequest and dynamoDbClient.getItem()
     *
     * 3. UPDATE an event:
     *    public void updateEvent(Event event) { ... }
     *    Hint: putItem() actually works for updates too!
     *
     * 4. FIND events by date range:
     *    public List<Event> findEventsByDateRange(String zipCode,
     *                                              LocalDateTime start,
     *                                              LocalDateTime end) { ... }
     *    Hint: Use a filter expression in your QueryRequest
     *
     * See README.md for complete code examples!
     */

}
