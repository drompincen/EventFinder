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

class EventFinderTest {

    private DynamoDbClient dynamoDbClient;
    private EventFinder eventFinder;

    @BeforeEach
    void setUp() {
        dynamoDbClient = mock(DynamoDbClient.class);
        eventFinder = new EventFinder(dynamoDbClient);
    }

    @Test
    void testSaveEvent() {
        Event event = new Event(
                UUID.randomUUID().toString(),
                "Test Event",
                "Test Location",
                "84098",
                "2025-09-20T10:00:00"
        );

        eventFinder.saveEvent(event);

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDbClient).putItem(captor.capture());

        Map<String, AttributeValue> item = captor.getValue().item();
        assertEquals("Test Event", item.get("name").s());
        assertEquals("84098", item.get("zipCode").s());
    }

    @Test
    void testFindEventsByZipCodeReturnsResults() {
        Map<String, AttributeValue> mockItem = Map.of(
                "id", AttributeValue.builder().s("abc123").build(),
                "name", AttributeValue.builder().s("Mock Event").build(),
                "location", AttributeValue.builder().s("Mock Location").build(),
                "zipCode", AttributeValue.builder().s("84098").build(),
                "date", AttributeValue.builder().s("2025-09-20T10:00:00").build()
        );

        QueryResponse response = QueryResponse.builder()
                .items(List.of(mockItem))
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

        List<Event> events = eventFinder.findEventsByZipCode("84098");

        assertEquals(1, events.size());
        assertEquals("Mock Event", events.get(0).getName());
    }

    @Test
    void testFindEventsByZipCodeTriggersAIOnEmpty() {
        QueryResponse emptyResponse = QueryResponse.builder()
                .items(List.of())
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(emptyResponse);

        List<Event> events = eventFinder.findEventsByZipCode("99999");

        assertFalse(events.isEmpty());
        assertTrue(events.get(0).getName().contains("Zip Jam")
                || events.get(0).getName().contains("Art & Wine")
                || events.get(0).getName().contains("Tech Meetup"));
    }
}