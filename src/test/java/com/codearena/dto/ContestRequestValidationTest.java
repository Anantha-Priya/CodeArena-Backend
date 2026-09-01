package com.codearena.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No controller uses ContestRequest yet (that's Phase 9), so this exercises the
 * EndTimeAfterStartTime constraint directly via the Validator rather than through HTTP.
 */
class ContestRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void endTimeBeforeStartTimeFailsValidation() {
        ContestRequest request = new ContestRequest(
            "Weekly Contest",
            "description",
            Instant.parse("2026-01-01T12:00:00Z"),
            Instant.parse("2026-01-01T10:00:00Z")
        );

        Set<ConstraintViolation<ContestRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("end_time must be after start_time");
    }

    @Test
    void endTimeAfterStartTimePassesValidation() {
        ContestRequest request = new ContestRequest(
            "Weekly Contest",
            "description",
            Instant.parse("2026-01-01T10:00:00Z"),
            Instant.parse("2026-01-01T12:00:00Z")
        );

        Set<ConstraintViolation<ContestRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

}
