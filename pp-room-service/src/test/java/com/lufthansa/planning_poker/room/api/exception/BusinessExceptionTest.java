package com.lufthansa.planning_poker.room.api.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessException Tests")
class BusinessExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message and error code")
        void shouldCreateWithMessageAndErrorCode() {
            // When
            BusinessException ex = new BusinessException("Test message", "TEST_ERROR");

            // Then
            assertThat(ex.getMessage()).isEqualTo("Test message");
            assertThat(ex.getErrorCode()).isEqualTo("TEST_ERROR");
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should create exception with message, error code and status")
        void shouldCreateWithMessageErrorCodeAndStatus() {
            // When
            BusinessException ex = new BusinessException("Custom error", "CUSTOM_ERROR", HttpStatus.UNPROCESSABLE_ENTITY);

            // Then
            assertThat(ex.getMessage()).isEqualTo("Custom error");
            assertThat(ex.getErrorCode()).isEqualTo("CUSTOM_ERROR");
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @Nested
    @DisplayName("Static Factory Methods")
    class StaticFactoryMethods {

        @Test
        @DisplayName("notFound should create NOT_FOUND exception with UUID id")
        void shouldCreateNotFoundWithUUID() {
            // Given
            UUID id = UUID.randomUUID();

            // When
            BusinessException ex = BusinessException.notFound("Room", id);

            // Then
            assertThat(ex.getMessage()).isEqualTo("Room not found with id: " + id);
            assertThat(ex.getErrorCode()).isEqualTo("ROOM_NOT_FOUND");
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("notFound should create NOT_FOUND exception with String id")
        void shouldCreateNotFoundWithStringId() {
            // When
            BusinessException ex = BusinessException.notFound("User", "user-123");

            // Then
            assertThat(ex.getMessage()).isEqualTo("User not found with id: user-123");
            assertThat(ex.getErrorCode()).isEqualTo("USER_NOT_FOUND");
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("notFound should handle different entity names")
        void shouldCreateNotFoundForDifferentEntities() {
            // Test with Story
            BusinessException storyEx = BusinessException.notFound("Story", "story-1");
            assertThat(storyEx.getErrorCode()).isEqualTo("STORY_NOT_FOUND");

            // Test with Participant
            BusinessException participantEx = BusinessException.notFound("Participant", "p-1");
            assertThat(participantEx.getErrorCode()).isEqualTo("PARTICIPANT_NOT_FOUND");

            // Test with Invitation
            BusinessException invitationEx = BusinessException.notFound("Invitation", "inv-1");
            assertThat(invitationEx.getErrorCode()).isEqualTo("INVITATION_NOT_FOUND");
        }

        @Test
        @DisplayName("forbidden should create FORBIDDEN exception")
        void shouldCreateForbidden() {
            // When
            BusinessException ex = BusinessException.forbidden("Only moderator can perform this action");

            // Then
            assertThat(ex.getMessage()).isEqualTo("Only moderator can perform this action");
            assertThat(ex.getErrorCode()).isEqualTo("FORBIDDEN");
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("conflict should create CONFLICT exception")
        void shouldCreateConflict() {
            // When
            BusinessException ex = BusinessException.conflict("Another story is already in voting");

            // Then
            assertThat(ex.getMessage()).isEqualTo("Another story is already in voting");
            assertThat(ex.getErrorCode()).isEqualTo("CONFLICT");
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("badRequest should create BAD_REQUEST exception")
        void shouldCreateBadRequest() {
            // When
            BusinessException ex = BusinessException.badRequest("Invalid deck type");

            // Then
            assertThat(ex.getMessage()).isEqualTo("Invalid deck type");
            assertThat(ex.getErrorCode()).isEqualTo("BAD_REQUEST");
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Exception Hierarchy")
    class ExceptionHierarchy {

        @Test
        @DisplayName("Should be a RuntimeException")
        void shouldBeRuntimeException() {
            // When
            BusinessException ex = BusinessException.badRequest("Test");

            // Then
            assertThat(ex).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should be throwable")
        void shouldBeThrowable() {
            // Given
            BusinessException ex = BusinessException.notFound("Entity", "1");

            // Then
            assertThat(ex).isInstanceOf(Throwable.class);
            assertThat(ex.getCause()).isNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty message")
        void shouldHandleEmptyMessage() {
            // When
            BusinessException ex = BusinessException.forbidden("");

            // Then
            assertThat(ex.getMessage()).isEmpty();
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should handle special characters in message")
        void shouldHandleSpecialCharacters() {
            // When
            BusinessException ex = BusinessException.badRequest("Invalid input: <script>alert('xss')</script>");

            // Then
            assertThat(ex.getMessage()).contains("<script>");
        }

        @Test
        @DisplayName("Should handle long entity names in notFound")
        void shouldHandleLongEntityNames() {
            // When
            BusinessException ex = BusinessException.notFound("VeryLongEntityNameForTesting", "123");

            // Then
            assertThat(ex.getErrorCode()).isEqualTo("VERYLONGENTITYNAMEFORTESTING_NOT_FOUND");
        }
    }
}

