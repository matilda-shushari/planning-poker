package com.lufthansa.planning_poker.room.api.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("handleBusinessException")
    class HandleBusinessException {

        @Test
        @DisplayName("Should return correct response for NOT_FOUND")
        void shouldHandleNotFound() {
            // Given
            BusinessException ex = BusinessException.notFound("Room", "123");

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBusinessException(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("ROOM_NOT_FOUND");
            assertThat(response.getBody().message()).contains("Room not found");
        }

        @Test
        @DisplayName("Should return correct response for FORBIDDEN")
        void shouldHandleForbidden() {
            // Given
            BusinessException ex = BusinessException.forbidden("Access denied");

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBusinessException(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("FORBIDDEN");
        }

        @Test
        @DisplayName("Should return correct response for CONFLICT")
        void shouldHandleConflict() {
            // Given
            BusinessException ex = BusinessException.conflict("Resource conflict");

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBusinessException(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("CONFLICT");
        }

        @Test
        @DisplayName("Should return correct response for BAD_REQUEST")
        void shouldHandleBadRequest() {
            // Given
            BusinessException ex = BusinessException.badRequest("Invalid input");

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBusinessException(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("BAD_REQUEST");
        }
    }

    @Nested
    @DisplayName("handleValidationException")
    class HandleValidationException {

        @Test
        @DisplayName("Should return BAD_REQUEST with field errors")
        void shouldHandleValidationErrors() {
            // Given
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            
            FieldError fieldError1 = new FieldError("request", "name", "Name is required");
            FieldError fieldError2 = new FieldError("request", "email", "Invalid email format");
            
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidationException(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getBody().details()).containsKey("name");
            assertThat(response.getBody().details()).containsKey("email");
        }
    }

    @Nested
    @DisplayName("handleConstraintViolation")
    class HandleConstraintViolation {

        @Test
        @DisplayName("Should return BAD_REQUEST with constraint violations")
        void shouldHandleConstraintViolations() {
            // Given
            Set<ConstraintViolation<?>> violations = new HashSet<>();
            
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn("fieldName");
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn("must not be null");
            violations.add(violation);

            ConstraintViolationException ex = new ConstraintViolationException(violations);

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleConstraintViolation(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("CONSTRAINT_VIOLATION");
            assertThat(response.getBody().details()).containsKey("fieldName");
        }
    }

    @Nested
    @DisplayName("handleAccessDenied")
    class HandleAccessDenied {

        @Test
        @DisplayName("Should return FORBIDDEN for access denied")
        void shouldHandleAccessDenied() {
            // Given
            AccessDeniedException ex = new AccessDeniedException("Access denied");

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAccessDenied(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("ACCESS_DENIED");
        }
    }

    @Nested
    @DisplayName("handleGenericException")
    class HandleGenericException {

        @Test
        @DisplayName("Should return INTERNAL_SERVER_ERROR for unexpected exceptions")
        void shouldHandleGenericException() {
            // Given
            Exception ex = new RuntimeException("Something went wrong");

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGenericException(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
            assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        }

        @Test
        @DisplayName("Should handle NullPointerException")
        void shouldHandleNullPointerException() {
            // Given
            Exception ex = new NullPointerException("Null reference");

            // When
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGenericException(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("ErrorResponse")
    class ErrorResponseTests {

        @Test
        @DisplayName("Should create ErrorResponse with status, code and message")
        void shouldCreateErrorResponseWithThreeParams() {
            // When
            GlobalExceptionHandler.ErrorResponse response = 
                new GlobalExceptionHandler.ErrorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");

            // Then
            assertThat(response.status()).isEqualTo(404);
            assertThat(response.error()).isEqualTo("Not Found");
            assertThat(response.errorCode()).isEqualTo("NOT_FOUND");
            assertThat(response.message()).isEqualTo("Resource not found");
            assertThat(response.details()).isNull();
            assertThat(response.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should create ErrorResponse with details")
        void shouldCreateErrorResponseWithDetails() {
            // Given
            var details = java.util.Map.of("field1", "error1", "field2", "error2");

            // When
            GlobalExceptionHandler.ErrorResponse response = 
                new GlobalExceptionHandler.ErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION", "Validation failed", details);

            // Then
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.details()).hasSize(2);
            assertThat(response.details()).containsEntry("field1", "error1");
        }
    }
}

