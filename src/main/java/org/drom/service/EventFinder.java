package org.drom.service;

import org.drom.data.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventFinder {

    private final DynamoDbClient dynamoDbClient;

    @Autowired
    public EventFinder(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public List<Event> findEventsByZipCode(String zipCode) {
        String tableName = "Events";

        Map<String, AttributeValue> expressionValues = Map.of(
                ":zip", AttributeValue.builder().s(zipCode).build()
        );

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("zipCode = :zip")
                .expressionAttributeValues(expressionValues)
                .build();

        try {
            QueryResponse response = dynamoDbClient.query(queryRequest);

            List<Event> events = response.items().stream()
                    .map(item -> new Event(
                            item.get("id").s(),
                            item.get("name").s(),
                            item.get("location").s(),
                            item.get("zipCode").s(),
                            item.get("date").s()
                    ))
                    .collect(Collectors.toList());

            return events.isEmpty() ? generateEventsWithAI(zipCode) : events;

        } catch (DynamoDbException e) {
            System.err.println("DynamoDB query failed: " + e.getMessage());
            return generateEventsWithAI(zipCode);
        }
    }

    private List<Event> generateEventsWithAI(String zipCode) {
        // Mock enrichment logic — replace with actual AI call if needed
        return List.of(
                new Event(UUID.randomUUID().toString(), "Zip Jam Festival", "Main Street Park", zipCode, LocalDateTime.now().plusDays(3).toString()),
                new Event(UUID.randomUUID().toString(), "Art & Wine Walk", "Historic District", zipCode, LocalDateTime.now().plusDays(7).toString()),
                new Event(UUID.randomUUID().toString(), "Tech Meetup", "Innovation Hub", zipCode, LocalDateTime.now().plusDays(10).toString())
        );
    }
    public void saveEvent(Event event) {
        Map<String, AttributeValue> item = Map.of(
                "id", AttributeValue.builder().s(event.getId()).build(),
                "name", AttributeValue.builder().s(event.getName()).build(),
                "location", AttributeValue.builder().s(event.getLocation()).build(),
                "zipCode", AttributeValue.builder().s(event.getZipCode()).build(),
                "date", AttributeValue.builder().s(String.valueOf(event.getDateTime())).build()
        );

        PutItemRequest request = PutItemRequest.builder()
                .tableName("Events")
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }

}