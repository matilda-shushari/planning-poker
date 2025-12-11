package com.lufthansa.planning_poker.room.domain.model;

import java.util.List;

/**
 * Supported deck types for Planning Poker estimation.
 */
public enum DeckType {
    SCRUM(List.of("0", "0.5", "1", "2", "3", "5", "8", "13", "20", "40", "100", "?", "☕")),
    FIBONACCI(List.of("0", "1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "?")),
    SEQUENTIAL(List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "?")),
    TSHIRT(List.of("XS", "S", "M", "L", "XL", "XXL", "?")),
    CUSTOM(List.of()); // User defines their own values

    private final List<String> defaultValues;

    DeckType(List<String> defaultValues) {
        this.defaultValues = defaultValues;
    }

    public List<String> getDefaultValues() {
        return defaultValues;
    }
}

