package net.tacia.backend.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for health checks and application state management
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationAvailability availability;
    
    public HealthController(ApplicationEventPublisher eventPublisher, ApplicationAvailability availability) {
        this.eventPublisher = eventPublisher;
        this.availability = availability;
        logger.info("HealthController initialized");
    }
    
    /**
     * Basic health check endpoint
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("liveness", availability.getLivenessState());
        response.put("readiness", availability.getReadinessState());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Liveness probe endpoint
     */
    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ALIVE");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Readiness probe endpoint
     */
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "READY");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint to change the liveness state (for testing)
     */
    @GetMapping("/liveness/state")
    public ResponseEntity<Map<String, String>> changeLivenessState(@RequestParam String state) {
        switch (state.toUpperCase()) {
            case "CORRECT":
                eventPublisher.publishEvent(new AvailabilityChangeEvent<>(this, LivenessState.CORRECT));
                break;
            case "BROKEN":
                eventPublisher.publishEvent(new AvailabilityChangeEvent<>(this, LivenessState.BROKEN));
                break;
            default:
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid state. Use 'CORRECT' or 'BROKEN'"));
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "Liveness state changed to " + state,
            "currentState", availability.getLivenessState().toString()
        ));
    }
    
    /**
     * Endpoint to change the readiness state (for testing)
     */
    @GetMapping("/readiness/state")
    public ResponseEntity<Map<String, String>> changeReadinessState(@RequestParam String state) {
        switch (state.toUpperCase()) {
            case "ACCEPTING_TRAFFIC":
                eventPublisher.publishEvent(new AvailabilityChangeEvent<>(this, ReadinessState.ACCEPTING_TRAFFIC));
                break;
            case "REFUSING_TRAFFIC":
                eventPublisher.publishEvent(new AvailabilityChangeEvent<>(this, ReadinessState.REFUSING_TRAFFIC));
                break;
            default:
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid state. Use 'ACCEPTING_TRAFFIC' or 'REFUSING_TRAFFIC'"));
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "Readiness state changed to " + state,
            "currentState", availability.getReadinessState().toString()
        ));
    }
}
