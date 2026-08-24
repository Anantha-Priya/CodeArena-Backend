package com.codearena.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void resourceNotFoundMapsTo404() {
        ResponseEntity<ErrorResponse> response =
            handler.handleResourceNotFound(new ResourceNotFoundException("Problem not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(404, "Problem not found"));
    }

    @Test
    void duplicateResourceMapsTo409() {
        ResponseEntity<ErrorResponse> response =
            handler.handleDuplicateResource(new DuplicateResourceException("Username already exists"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(409, "Username already exists"));
    }

    @Test
    void unauthorizedMapsTo401() {
        ResponseEntity<ErrorResponse> response =
            handler.handleUnauthorized(new UnauthorizedException("Invalid email or password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(401, "Invalid email or password"));
    }

    @Test
    void badRequestMapsTo400() {
        ResponseEntity<ErrorResponse> response =
            handler.handleBadRequest(new BadRequestException("Malformed request"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(400, "Malformed request"));
    }

}
