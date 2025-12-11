package com.lufthansa.planning_poker.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/room")
    public Mono<ResponseEntity<Map<String, Object>>> roomServiceFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createFallbackResponse("Room Service")));
    }

    @GetMapping("/vote")
    public Mono<ResponseEntity<Map<String, Object>>> voteServiceFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createFallbackResponse("Vote Service")));
    }

    @GetMapping("/audit")
    public Mono<ResponseEntity<Map<String, Object>>> auditServiceFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createFallbackResponse("Audit Service")));
    }

    private Map<String, Object> createFallbackResponse(String serviceName) {
        return Map.of(
            "timestamp", Instant.now().toString(),
            "status", 503,
            "error", "Service Unavailable",
            "message", serviceName + " is currently unavailable. Please try again later.",
            "errorCode", "SERVICE_UNAVAILABLE"
        );
    }
}

