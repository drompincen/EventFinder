package org.drom.controller;

import org.drom.data.Event;
import org.drom.service.EventFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventFinder eventFinder;

    @Autowired
    public EventController(EventFinder eventFinder) {
        this.eventFinder = eventFinder;
    }

    @GetMapping
    public List<Event> getEventsByZip(@RequestParam @Pattern(regexp = "\\d{5}") String zip) {
        return eventFinder.findEventsByZipCode(zip);
    }
}