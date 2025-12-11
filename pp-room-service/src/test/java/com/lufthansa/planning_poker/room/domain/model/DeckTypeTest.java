package com.lufthansa.planning_poker.room.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeckType Tests")
class DeckTypeTest {

    @Nested
    @DisplayName("SCRUM Deck")
    class ScrumDeck {

        @Test
        @DisplayName("Should have correct default values")
        void shouldHaveCorrectDefaultValues() {
            // When
            List<String> values = DeckType.SCRUM.getDefaultValues();

            // Then
            assertThat(values).containsExactly(
                "0", "0.5", "1", "2", "3", "5", "8", "13", "20", "40", "100", "?", "☕"
            );
        }

        @Test
        @DisplayName("Should contain coffee break emoji")
        void shouldContainCoffeeBreak() {
            assertThat(DeckType.SCRUM.getDefaultValues()).contains("☕");
        }

        @Test
        @DisplayName("Should contain half point")
        void shouldContainHalfPoint() {
            assertThat(DeckType.SCRUM.getDefaultValues()).contains("0.5");
        }
    }

    @Nested
    @DisplayName("FIBONACCI Deck")
    class FibonacciDeck {

        @Test
        @DisplayName("Should have Fibonacci sequence values")
        void shouldHaveFibonacciValues() {
            // When
            List<String> values = DeckType.FIBONACCI.getDefaultValues();

            // Then
            assertThat(values).containsExactly(
                "0", "1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "?"
            );
        }

        @Test
        @DisplayName("Should follow Fibonacci pattern")
        void shouldFollowFibonacciPattern() {
            List<String> values = DeckType.FIBONACCI.getDefaultValues();
            
            // Check some Fibonacci numbers (skipping 0, 1, 2 which are special)
            assertThat(values).contains("5");  // 2+3
            assertThat(values).contains("8");  // 3+5
            assertThat(values).contains("13"); // 5+8
            assertThat(values).contains("21"); // 8+13
            assertThat(values).contains("34"); // 13+21
            assertThat(values).contains("55"); // 21+34
            assertThat(values).contains("89"); // 34+55
        }
    }

    @Nested
    @DisplayName("SEQUENTIAL Deck")
    class SequentialDeck {

        @Test
        @DisplayName("Should have sequential values from 0 to 10")
        void shouldHaveSequentialValues() {
            // When
            List<String> values = DeckType.SEQUENTIAL.getDefaultValues();

            // Then
            assertThat(values).containsExactly(
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "?"
            );
        }

        @Test
        @DisplayName("Should have 12 values including question mark")
        void shouldHave12Values() {
            assertThat(DeckType.SEQUENTIAL.getDefaultValues()).hasSize(12);
        }
    }

    @Nested
    @DisplayName("TSHIRT Deck")
    class TShirtDeck {

        @Test
        @DisplayName("Should have t-shirt size values")
        void shouldHaveTShirtSizes() {
            // When
            List<String> values = DeckType.TSHIRT.getDefaultValues();

            // Then
            assertThat(values).containsExactly("XS", "S", "M", "L", "XL", "XXL", "?");
        }

        @Test
        @DisplayName("Should be in ascending size order")
        void shouldBeInAscendingOrder() {
            List<String> values = DeckType.TSHIRT.getDefaultValues();
            
            assertThat(values.indexOf("XS")).isLessThan(values.indexOf("S"));
            assertThat(values.indexOf("S")).isLessThan(values.indexOf("M"));
            assertThat(values.indexOf("M")).isLessThan(values.indexOf("L"));
            assertThat(values.indexOf("L")).isLessThan(values.indexOf("XL"));
            assertThat(values.indexOf("XL")).isLessThan(values.indexOf("XXL"));
        }
    }

    @Nested
    @DisplayName("CUSTOM Deck")
    class CustomDeck {

        @Test
        @DisplayName("Should have empty default values")
        void shouldHaveEmptyDefaults() {
            assertThat(DeckType.CUSTOM.getDefaultValues()).isEmpty();
        }
    }

    @Nested
    @DisplayName("All Deck Types")
    class AllDeckTypes {

        @ParameterizedTest
        @EnumSource(DeckType.class)
        @DisplayName("Should return non-null default values for all types")
        void shouldReturnNonNullValues(DeckType deckType) {
            assertThat(deckType.getDefaultValues()).isNotNull();
        }

        @ParameterizedTest
        @EnumSource(value = DeckType.class, names = {"SCRUM", "FIBONACCI", "SEQUENTIAL", "TSHIRT"})
        @DisplayName("Non-custom deck types should contain question mark")
        void shouldContainQuestionMark(DeckType deckType) {
            assertThat(deckType.getDefaultValues()).contains("?");
        }

        @Test
        @DisplayName("Should have exactly 5 deck types")
        void shouldHaveFiveTypes() {
            assertThat(DeckType.values()).hasSize(5);
        }

        @Test
        @DisplayName("Should be able to get deck type by name")
        void shouldGetByName() {
            assertThat(DeckType.valueOf("FIBONACCI")).isEqualTo(DeckType.FIBONACCI);
            assertThat(DeckType.valueOf("SCRUM")).isEqualTo(DeckType.SCRUM);
            assertThat(DeckType.valueOf("TSHIRT")).isEqualTo(DeckType.TSHIRT);
        }
    }
}

