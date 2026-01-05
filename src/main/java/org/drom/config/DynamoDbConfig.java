package org.drom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * DYNAMODB CONFIGURATION - Setting Up Our Database Connection
 * 
 * This class tells Spring how to connect to DynamoDB (our database).
 * Think of it like setting up the WiFi password - you do it once,
 * and then everything can connect automatically.
 * 
 * WHAT IS DYNAMODB?
 * DynamoDB is Amazon's NoSQL database. Unlike traditional databases
 * with tables and rows (like Excel), NoSQL databases are more flexible
 * and can handle massive amounts of data very quickly.
 * 
 * ANNOTATIONS EXPLAINED:
 * 
 * @Configuration - Tells Spring: "This class contains setup/configuration code.
 *                  Run this when the application starts."
 * 
 * @Bean - Tells Spring: "The object returned by this method should be 
 *         managed by Spring. When anyone needs a DynamoDbClient, give 
 *         them this one."
 * 
 * WHY USE @Bean?
 * Instead of creating new DynamoDbClient() everywhere in our code,
 * we create ONE instance here, and Spring shares it with everyone
 * who needs it. This is called the "Singleton" pattern.
 * 
 * ═══════════════════════════════════════════════════════════════════
 * IMPORTANT: This current setup connects to REAL AWS!
 * For local development, see the README.md for how to set up
 * DynamoDB Local with Docker and use Spring Profiles.
 * ═══════════════════════════════════════════════════════════════════
 */
@Configuration
public class DynamoDbConfig {

    /**
     * Creates and configures the DynamoDB client.
     * 
     * This client is our "connection" to DynamoDB. We use it to:
     *   - Save data (putItem)
     *   - Read data (getItem, query)
     *   - Delete data (deleteItem)
     * 
     * The Builder Pattern:
     * Notice how we use .builder() ... .build()? This is called the 
     * "Builder Pattern". It's a clean way to create objects with many
     * optional settings. Instead of a constructor with 10 parameters,
     * we chain method calls that read like English.
     * 
     * @return A configured DynamoDbClient ready to talk to AWS
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.US_WEST_2)  // AWS region where our database lives
                                            // US_WEST_2 = Oregon data center
                                            // Change this to match your AWS setup!
                .build();
    }
    
    /*
     * ═══════════════════════════════════════════════════════════════
     * CHALLENGE: Add support for LOCAL development!
     * ═══════════════════════════════════════════════════════════════
     * 
     * For local development with DynamoDB Local (Docker), you'll want
     * to add a second @Bean method with @Profile("local"):
     * 
     * @Bean
     * @Profile("local")  // Only used when running with -Dspring.profiles.active=local
     * public DynamoDbClient dynamoDbClientLocal() {
     *     return DynamoDbClient.builder()
     *             .endpointOverride(URI.create("http://localhost:8000"))
     *             .region(Region.US_WEST_2)
     *             .credentialsProvider(StaticCredentialsProvider.create(
     *                     AwsBasicCredentials.create("fakeKey", "fakeSecret")))
     *             .build();
     * }
     * 
     * Then add @Profile("!local") to the existing method above.
     * See README.md for the complete setup!
     */
}
