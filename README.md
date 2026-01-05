# Event Finder 🎉

A Spring Boot application for discovering and managing local events by zip code. This project uses AWS DynamoDB for data storage and is a great way to learn about REST APIs, Spring Boot, and cloud databases.

## Table of Contents
- [Project Overview](#project-overview)
- [Prerequisites](#prerequisites)
- [Part 1: Local Development with DynamoDB Local](#part-1-local-development-with-dynamodb-local)
- [Part 2: Understanding the Code](#part-2-understanding-the-code)
- [Part 3: Adding New Features](#part-3-adding-new-features)
- [Part 4: Testing Your Code](#part-4-testing-your-code)
- [Part 5: Deploying to AWS](#part-5-deploying-to-aws)
- [API Reference](#api-reference)
- [Troubleshooting](#troubleshooting)

---

## Project Overview

This application allows users to:
- **Find events** by zip code
- **Save new events** to the database
- **Auto-generate mock events** when no events exist for a zip code

### Tech Stack
- **Java 17** - Programming language
- **Spring Boot 3.1** - Web framework
- **AWS DynamoDB** - NoSQL database
- **Maven** - Build tool
- **Lombok** - Reduces boilerplate code
- **JUnit 5 + Mockito** - Testing

---

## Prerequisites

Make sure you have these installed:

```bash
# Check Java version (need 17+)
java -version

# Check Maven
mvn -version

# Check Docker (for DynamoDB Local)
docker --version
```

If you don't have Docker, install it from [docker.com](https://www.docker.com/get-started).

---

## Part 1: Local Development with DynamoDB Local

Instead of connecting to real AWS (which costs money), we'll use **DynamoDB Local** - a free Docker container that simulates DynamoDB on your computer.

### Step 1: Start DynamoDB Local

```bash
# Pull and run DynamoDB Local
docker run -d -p 8000:8000 --name dynamodb-local amazon/dynamodb-local

# Verify it's running
docker ps
```

You should see `dynamodb-local` in the list.

### Step 2: Create the Events Table

DynamoDB needs a table before we can store data. Run this command:

```bash
aws dynamodb create-table \
    --table-name Events \
    --attribute-definitions \
        AttributeName=zipCode,AttributeType=S \
        AttributeName=id,AttributeType=S \
    --key-schema \
        AttributeName=zipCode,KeyType=HASH \
        AttributeName=id,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --endpoint-url http://localhost:8000
```

**Don't have AWS CLI?** Install it:
```bash
# On Ubuntu/Debian
sudo apt-get install awscli

# Or with pip
pip install awscli

# Configure with dummy credentials (required even for local)
aws configure
# Access Key ID: fakeAccessKey
# Secret Access Key: fakeSecretKey
# Region: us-west-2
# Output format: json
```

### Step 3: Update the Application for Local Mode

We need to tell Spring Boot to connect to our local DynamoDB instead of AWS.

**Option A: Using a Spring Profile (Recommended)**

Create a new file `src/main/resources/application-local.properties`:

```properties
# Local DynamoDB configuration
aws.dynamodb.endpoint=http://localhost:8000
aws.region=us-west-2
```

Then modify `src/main/java/org/drom/config/DynamoDbConfig.java`:

```java
package org.drom.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    // LOCAL PROFILE - connects to DynamoDB Local
    @Bean
    @Profile("local")
    public DynamoDbClient dynamoDbClientLocal() {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:8000"))
                .region(Region.US_WEST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("fakeAccessKey", "fakeSecretKey")))
                .build();
    }

    // PRODUCTION PROFILE - connects to real AWS
    @Bean
    @Profile("!local")
    public DynamoDbClient dynamoDbClientProd() {
        return DynamoDbClient.builder()
                .region(Region.US_WEST_2)
                .build();
    }
}
```

### Step 4: Run the Application Locally

```bash
# Build the project
mvn clean compile

# Run with local profile
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The app will start on `http://localhost:8080`.

### Step 5: Test the API

```bash
# Find events by zip code (will return mock events if none exist)
curl "http://localhost:8080/events?zip=84098"

# You should see JSON with events!
```

---

## Part 2: Understanding the Code

### Project Structure

```
src/main/java/org/drom/
├── App.java                    # Main entry point
├── config/
│   └── DynamoDbConfig.java     # Database configuration
├── controller/
│   └── EventController.java    # REST API endpoints
├── data/
│   └── Event.java              # Event data model
└── service/
    └── EventFinder.java        # Business logic
```

### How It Works

1. **Controller** (`EventController.java`) - Receives HTTP requests
2. **Service** (`EventFinder.java`) - Contains business logic
3. **Config** (`DynamoDbConfig.java`) - Sets up database connection
4. **Model** (`Event.java`) - Defines what an Event looks like

### The Event Model

```java
public class Event {
    private String id;           // Unique identifier (UUID)
    private String name;         // Event title
    private String description;  // Short summary
    private LocalDateTime dateTime;  // When it's happening
    private String location;     // Venue or address
    private String zipCode;      // For filtering (this is the partition key!)
    private String category;     // e.g., Concert, Market
    private String sourceUrl;    // Link to more info
}
```

---

## Part 3: Adding New Features

Here are some features you can add to practice:

### Challenge 1: Add a POST Endpoint to Create Events

The `saveEvent()` method already exists in `EventFinder.java`. You just need to expose it via the controller!

**In `EventController.java`, add:**

```java
@PostMapping
public Event createEvent(@RequestBody Event event) {
    // Generate a unique ID if not provided
    if (event.getId() == null || event.getId().isEmpty()) {
        event.setId(UUID.randomUUID().toString());
    }
    eventFinder.saveEvent(event);
    return event;
}
```

Don't forget to add the import:
```java
import java.util.UUID;
```

**Test it:**
```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Summer Concert",
    "location": "Central Park",
    "zipCode": "84098",
    "dateTime": "2025-07-15T19:00:00",
    "category": "Concert"
  }'
```

### Challenge 2: Add a DELETE Endpoint

**In `EventFinder.java`, add:**

```java
public void deleteEvent(String zipCode, String id) {
    Map<String, AttributeValue> key = Map.of(
            "zipCode", AttributeValue.builder().s(zipCode).build(),
            "id", AttributeValue.builder().s(id).build()
    );

    DeleteItemRequest request = DeleteItemRequest.builder()
            .tableName("Events")
            .key(key)
            .build();

    dynamoDbClient.deleteItem(request);
}
```

**In `EventController.java`, add:**

```java
@DeleteMapping("/{zipCode}/{id}")
public void deleteEvent(@PathVariable String zipCode, @PathVariable String id) {
    eventFinder.deleteEvent(zipCode, id);
}
```

Add the import for `DeleteItemRequest`:
```java
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
```

### Challenge 3: Add a GET Endpoint for a Single Event

**In `EventFinder.java`, add:**

```java
public Event getEvent(String zipCode, String id) {
    Map<String, AttributeValue> key = Map.of(
            "zipCode", AttributeValue.builder().s(zipCode).build(),
            "id", AttributeValue.builder().s(id).build()
    );

    GetItemRequest request = GetItemRequest.builder()
            .tableName("Events")
            .key(key)
            .build();

    GetItemResponse response = dynamoDbClient.getItem(request);
    
    if (response.item() == null || response.item().isEmpty()) {
        return null;
    }

    Map<String, AttributeValue> item = response.item();
    return new Event(
            item.get("id").s(),
            item.get("name").s(),
            item.get("location").s(),
            item.get("zipCode").s(),
            item.get("date").s()
    );
}
```

**In `EventController.java`, add:**

```java
@GetMapping("/{zipCode}/{id}")
public Event getEvent(@PathVariable String zipCode, @PathVariable String id) {
    return eventFinder.getEvent(zipCode, id);
}
```

---

## Part 4: Testing Your Code

### Running Existing Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=EventFinderTest
```

### Writing New Tests

When you add new methods, write tests for them! Here's an example for the delete method:

**In `src/test/java/org/drom/service/EventFinderTest.java`, add:**

```java
@Test
void testDeleteEvent() {
    String zipCode = "84098";
    String id = "abc123";

    eventFinder.deleteEvent(zipCode, id);

    ArgumentCaptor<DeleteItemRequest> captor = ArgumentCaptor.forClass(DeleteItemRequest.class);
    verify(dynamoDbClient).deleteItem(captor.capture());

    Map<String, AttributeValue> key = captor.getValue().key();
    assertEquals(zipCode, key.get("zipCode").s());
    assertEquals(id, key.get("id").s());
}
```

### Testing Tips

1. **Unit Tests** use Mockito to mock the database - they're fast and don't need DynamoDB running
2. **Integration Tests** test the real database connection - run these with DynamoDB Local
3. Always test both **happy path** (things work) and **error cases** (things fail)

---

## Part 5: Deploying to AWS

Once you're ready to use real AWS DynamoDB:

### Step 1: Create an AWS Account

Go to [aws.amazon.com](https://aws.amazon.com) and create a free tier account.

### Step 2: Create an IAM User

1. Go to **IAM** in AWS Console
2. Create a new user with **Programmatic access**
3. Attach the policy: `AmazonDynamoDBFullAccess`
4. Save the **Access Key ID** and **Secret Access Key** securely!

⚠️ **NEVER commit these keys to Git!**

### Step 3: Create the DynamoDB Table in AWS

```bash
aws dynamodb create-table \
    --table-name Events \
    --attribute-definitions \
        AttributeName=zipCode,AttributeType=S \
        AttributeName=id,AttributeType=S \
    --key-schema \
        AttributeName=zipCode,KeyType=HASH \
        AttributeName=id,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --region us-west-2
```

### Step 4: Configure Credentials Safely

**Option A: Environment Variables (Recommended for local dev)**

```bash
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_REGION=us-west-2
```

**Option B: AWS Credentials File**

Create `~/.aws/credentials`:
```
[default]
aws_access_key_id = your_access_key
aws_secret_access_key = your_secret_key
```

And `~/.aws/config`:
```
[default]
region = us-west-2
```

**Option C: Using .env file with direnv (Best for projects)**

1. Create `.env` in project root:
```bash
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_REGION=us-west-2
```

2. Add `.env` to `.gitignore`:
```bash
echo ".env" >> .gitignore
```

3. Load it before running:
```bash
source .env
mvn spring-boot:run
```

### Step 5: Run in Production Mode

```bash
# Without the local profile, it connects to real AWS
mvn spring-boot:run
```

---

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/events?zip={zipCode}` | Find events by zip code |
| GET | `/events/{zipCode}/{id}` | Get a specific event |
| POST | `/events` | Create a new event |
| DELETE | `/events/{zipCode}/{id}` | Delete an event |

### Example Requests

**Find events:**
```bash
curl "http://localhost:8080/events?zip=84098"
```

**Create event:**
```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Local Farmers Market",
    "location": "Town Square",
    "zipCode": "84098",
    "dateTime": "2025-06-01T09:00:00",
    "category": "Market",
    "description": "Fresh local produce every Saturday!"
  }'
```

---

## Troubleshooting

### "Connection refused" error
- Make sure DynamoDB Local is running: `docker ps`
- Start it if needed: `docker start dynamodb-local`

### "Table not found" error
- Create the table using the command in Step 2 of Part 1

### "Credentials not found" error
- For local: Make sure you're using the `local` profile
- For AWS: Check your environment variables or ~/.aws/credentials

### Tests failing
- Run `mvn clean test` to rebuild everything
- Check that you have the right imports

### Application won't start
- Check for port conflicts: `lsof -i :8080`
- Kill any process using port 8080 or change the port in application.properties

---

## Next Steps

Once you've mastered this project, try:
1. Adding **pagination** for large result sets
2. Adding **search by date range**
3. Adding **event categories** filtering
4. Building a **frontend** with React or Vue
5. Adding **user authentication**

Happy coding! 🚀
