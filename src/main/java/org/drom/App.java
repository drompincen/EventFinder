package org.drom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is the MAIN ENTRY POINT of our application.
 * 
 * Think of this class like the "power button" for our web server.
 * When you run this class, it starts up the entire application.
 * 
 * @SpringBootApplication is a special annotation that tells Spring Boot:
 *   1. "This is a Spring Boot app"
 *   2. "Automatically find and set up all my other classes"
 *   3. "Configure everything with sensible defaults"
 * 
 * It's like magic - Spring Boot does a lot of work behind the scenes!
 */
@SpringBootApplication
public class App {
    
    /**
     * The main method - where Java programs always start.
     * 
     * SpringApplication.run() does all the heavy lifting:
     *   - Starts an embedded web server (Tomcat)
     *   - Scans for controllers, services, and configs
     *   - Sets up the database connection
     *   - Makes our API available at http://localhost:8080
     * 
     * @param args Command line arguments (we don't use these, but Java requires them)
     */
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}