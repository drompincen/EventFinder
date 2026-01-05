package org.drom.controller;

import org.drom.data.Event;
import org.drom.service.EventFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * EVENT CONTROLLER - The "Front Desk" of our API
 * 
 * This class handles all incoming HTTP requests related to events.
 * Think of it like a receptionist who:
 *   1. Receives requests from users (via their browser or app)
 *   2. Figures out what they want
 *   3. Asks the right department (Service) to do the work
 *   4. Sends back the response
 * 
 * ANNOTATIONS EXPLAINED:
 * 
 * @RestController - Tells Spring: "This class handles web requests and 
 *                   returns data (usually JSON), not HTML pages"
 * 
 * @RequestMapping("/events") - All URLs in this controller start with /events
 *                              So our base URL is: http://localhost:8080/events
 * 
 * HOW WEB REQUESTS WORK:
 * 
 *   User's Browser                    Our Server
 *   ─────────────                    ──────────
 *        │                                │
 *        │  GET /events?zip=84098         │
 *        │ ─────────────────────────────► │
 *        │                                │  (Controller receives request)
 *        │                                │  (Calls EventFinder service)
 *        │                                │  (Service queries database)
 *        │                                │
 *        │    JSON response with events   │
 *        │ ◄───────────────────────────── │
 *        │                                │
 */
@RestController
@RequestMapping("/events")
public class EventController {

    /**
     * The EventFinder service that does the actual work.
     * Controllers should be "thin" - they just direct traffic.
     * The real business logic lives in the Service layer.
     */
    private final EventFinder eventFinder;

    /**
     * CONSTRUCTOR with Dependency Injection
     * 
     * @Autowired tells Spring: "Hey, I need an EventFinder to work. 
     * Please find one and give it to me!"
     * 
     * Spring automatically creates an EventFinder instance and "injects" 
     * it here. This is called "Dependency Injection" - one of the most 
     * important concepts in Spring!
     * 
     * Why is this useful?
     *   - We don't have to create EventFinder ourselves
     *   - Spring manages the lifecycle of our objects
     *   - Makes testing easier (we can inject mock objects)
     * 
     * @param eventFinder The service that handles event operations
     */
    @Autowired
    public EventController(EventFinder eventFinder) {
        this.eventFinder = eventFinder;
    }

    /**
     * GET EVENTS BY ZIP CODE
     * 
     * HTTP Method: GET
     * URL: /events?zip=84098
     * 
     * Example: curl "http://localhost:8080/events?zip=84098"
     * 
     * @GetMapping - Handles HTTP GET requests (used for READING data)
     *               GET is like asking "Can I see this information?"
     * 
     * @RequestParam - Extracts the "zip" parameter from the URL
     *                 In /events?zip=84098, it grabs "84098"
     * 
     * @Pattern(regexp = "\\d{5}") - VALIDATION! Ensures zip is exactly 5 digits
     *                               \\d means "any digit 0-9"
     *                               {5} means "exactly 5 times"
     *                               So "84098" ✓ but "abc" ✗ and "123" ✗
     * 
     * @param zip The 5-digit zip code to search for
     * @return A list of Event objects (automatically converted to JSON!)
     */
    @GetMapping
    public List<Event> getEventsByZip(@RequestParam @Pattern(regexp = "\\d{5}") String zip) {
        // Delegate the work to our service layer
        // The controller's job is just to receive the request and return the response
        return eventFinder.findEventsByZipCode(zip);
    }
    
    /*
     * ═══════════════════════════════════════════════════════════════
     * CHALLENGE: Add more endpoints! Here are some ideas:
     * ═══════════════════════════════════════════════════════════════
     * 
     * 1. POST /events - Create a new event
     *    @PostMapping
     *    public Event createEvent(@RequestBody Event event) { ... }
     * 
     * 2. DELETE /events/{zipCode}/{id} - Delete an event
     *    @DeleteMapping("/{zipCode}/{id}")
     *    public void deleteEvent(@PathVariable String zipCode, @PathVariable String id) { ... }
     * 
     * 3. GET /events/{zipCode}/{id} - Get a single event
     *    @GetMapping("/{zipCode}/{id}")
     *    public Event getEvent(@PathVariable String zipCode, @PathVariable String id) { ... }
     * 
     * See the README.md for complete code examples!
     */
}