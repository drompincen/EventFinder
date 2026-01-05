package org.drom.service;

import org.drom.data.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UNIT TESTS FOR EVENT FINDER SERVICE
 *
 * ═══════════════════════════════════════════════════════════════════════
 *                        WHY DO WE WRITE TESTS?
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Tests are like a safety net for your code. They help you:
 *   1. Catch bugs BEFORE they reach users
 *   2. Refactor code confidently (if tests pass, you didn't break anything)
 *   3. Document how your code should behave
 *   4. Sleep better at night 😴
 *
 * ═══════════════════════════════════════════════════════════════════════
 *                        WHAT IS MOCKING?
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Our EventFinder needs a DynamoDB database to work. But in tests, we
 * don't want to use a REAL database because:
 *   - It's slow (network calls take time)
 *   - It's unreliable (what if AWS is down?)
 *   - It costs money (AWS charges for usage)
 *   - It's hard to control (we can't predict what's in the database)
 *
 * Instead, we create a "mock" - a FAKE DynamoDbClient that:
 *   - Pretends to be the real thing
 *   - Returns whatever data we tell it to
 *   - Runs instantly (no network calls)
 *   - Is 100% predictable
 *
 * MOCKITO is the library we use to create mocks.
 *
 * ═══════════════════════════════════════════════════════════════════════
 *                        TEST STRUCTURE: AAA
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Good tests follow the AAA pattern:
 *   - ARRANGE: Set up the test data and mocks
 *   - ACT: Call the method you're testing
 *   - ASSERT: Check that the result is correct
 */
class EventFinderTest {

    // The mock database client - it's FAKE, not real!
    private DynamoDbClient dynamoDbClient;

    // The class we're testing - this is REAL
    private EventFinder eventFinder;

    /**
     * SET UP - Runs BEFORE each test method
     *
     * @BeforeEach means: "Run this method before every single test"
     *
     * This ensures each test starts with a fresh mock and a fresh
     * EventFinder. Tests should be INDEPENDENT - one test shouldn't
     * affect another!
     */
    @BeforeEach
    void setUp() {
        // Create a FAKE DynamoDbClient using Mockito
        // mock() creates an object that looks like DynamoDbClient
        // but doesn't actually do anything until we tell it to
        dynamoDbClient = mock(DynamoDbClient.class);

        // Create a REAL EventFinder, but give it our FAKE database
        // This is called "dependency injection" - we're injecting
        // a mock dependency for testing purposes
        eventFinder = new EventFinder(dynamoDbClient);
    }

    // ═══════════════════════════════════════════════════════════════════
    //                         TEST: SAVE EVENT
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Test that saveEvent() correctly saves an event to DynamoDB.
     *
     * @Test marks this as a test method. JUnit will run it automatically.
     *
     * This test verifies:
     *   1. saveEvent() calls the database's putItem() method
     *   2. The data sent to the database is correct
     */
    @Test
    void testSaveEvent() {
        // ═══════════════════════════════════════════════════════════════
        // ARRANGE: Create test data
        // ═══════════════════════════════════════════════════════════════
        Event event = new Event(
                UUID.randomUUID().toString(),  // Random ID
                "Test Event",                   // Name
                "Test Location",                // Location
                "84098",                        // Zip code
                "2025-09-20T10:00:00"          // Date
        );

        // ═══════════════════════════════════════════════════════════════
        // ACT: Call the method we're testing
        // ═══════════════════════════════════════════════════════════════
        eventFinder.saveEvent(event);

        // ═══════════════════════════════════════════════════════════════
        // ASSERT: Verify the database was called correctly
        // ═══════════════════════════════════════════════════════════════

        /*
         * ArgumentCaptor is like a spy that captures what was sent to the mock.
         *
         * When saveEvent() calls dynamoDbClient.putItem(request), the captor
         * grabs that 'request' so we can inspect it.
         */
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);

        /*
         * verify() checks that a method was called on our mock.
         *
         * This line says: "Make sure putItem() was called exactly once,
         * and capture whatever was passed to it."
         *
         * If putItem() was never called, this test FAILS!
         */
        verify(dynamoDbClient).putItem(captor.capture());

        // Get the data that was sent to the database
        Map<String, AttributeValue> item = captor.getValue().item();

        /*
         * assertEquals(expected, actual) checks if two values are equal.
         * If they're not equal, the test FAILS with a helpful message.
         */
        assertEquals("Test Event", item.get("name").s());
        assertEquals("84098", item.get("zipCode").s());
    }

    // ═══════════════════════════════════════════════════════════════════
    //                   TEST: FIND EVENTS (with results)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Test that findEventsByZipCode() returns events when the database has data.
     *
     * This test:
     *   1. Sets up the mock to return fake event data
     *   2. Calls findEventsByZipCode()
     *   3. Verifies we get the expected events back
     */
    @Test
    void testFindEventsByZipCodeReturnsResults() {
        // ═══════════════════════════════════════════════════════════════
        // ARRANGE: Set up fake database response
        // ═══════════════════════════════════════════════════════════════

        // Create a fake "row" of data that looks like what DynamoDB returns
        Map<String, AttributeValue> mockItem = Map.of(
                "id", AttributeValue.builder().s("abc123").build(),
                "name", AttributeValue.builder().s("Mock Event").build(),
                "location", AttributeValue.builder().s("Mock Location").build(),
                "zipCode", AttributeValue.builder().s("84098").build(),
                "date", AttributeValue.builder().s("2025-09-20T10:00:00").build()
        );

        // Wrap it in a QueryResponse (what DynamoDB actually returns)
        QueryResponse response = QueryResponse.builder()
                .items(List.of(mockItem))  // List with one item
                .build();

        /*
         * STUBBING - Tell the mock what to return
         *
         * when(...).thenReturn(...) is Mockito's way of saying:
         * "When someone calls query() with ANY QueryRequest,
         *  return this fake response instead of calling the real database."
         *
         * any(QueryRequest.class) means "match any QueryRequest argument"
         */
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

        // ═══════════════════════════════════════════════════════════════
        // ACT: Call the method we're testing
        // ═══════════════════════════════════════════════════════════════
        List<Event> events = eventFinder.findEventsByZipCode("84098");

        // ═══════════════════════════════════════════════════════════════
        // ASSERT: Check the results
        // ═══════════════════════════════════════════════════════════════
        assertEquals(1, events.size());                    // Should have 1 event
        assertEquals("Mock Event", events.get(0).getName()); // Name should match
    }

    // ═══════════════════════════════════════════════════════════════════
    //                   TEST: FIND EVENTS (empty - triggers fallback)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Test that findEventsByZipCode() returns mock events when database is empty.
     *
     * This tests the "fallback" behavior - when there are no real events,
     * the system should generate some mock events so users see something.
     *
     * This is a great example of testing "edge cases" - what happens when
     * things don't go as expected?
     */
    @Test
    void testFindEventsByZipCodeTriggersAIOnEmpty() {
        // ═══════════════════════════════════════════════════════════════
        // ARRANGE: Set up mock to return EMPTY results
        // ═══════════════════════════════════════════════════════════════
        QueryResponse emptyResponse = QueryResponse.builder()
                .items(List.of())  // Empty list - no events in database!
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(emptyResponse);

        // ═══════════════════════════════════════════════════════════════
        // ACT: Search for events in a zip code with no data
        // ═══════════════════════════════════════════════════════════════
        List<Event> events = eventFinder.findEventsByZipCode("99999");

        // ═══════════════════════════════════════════════════════════════
        // ASSERT: Should get mock/generated events, not an empty list
        // ═══════════════════════════════════════════════════════════════

        // assertFalse() checks that the condition is FALSE
        // We're saying: "The list should NOT be empty"
        assertFalse(events.isEmpty());

        // assertTrue() checks that the condition is TRUE
        // We're checking that we got one of our known mock events
        // The || means "OR" - any of these names is acceptable
        assertTrue(events.get(0).getName().contains("Zip Jam")
                || events.get(0).getName().contains("Art & Wine")
                || events.get(0).getName().contains("Tech Meetup"));
    }

    /*
     * ═══════════════════════════════════════════════════════════════════
     * CHALLENGE: Write more tests!
     * ═══════════════════════════════════════════════════════════════════
     *
     * Good things to test:
     *
     * 1. What happens when the database throws an exception?
     *    Hint: when(...).thenThrow(new DynamoDbException(...))
     *
     * 2. Test the deleteEvent() method (once you implement it)
     *
     * 3. Test with multiple events in the response
     *
     * 4. Test with invalid zip codes
     *
     * Remember: A good test suite gives you confidence to change code!
     */
}
