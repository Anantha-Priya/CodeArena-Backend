package com.codearena.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EndTimeAfterStartTimeValidator implements ConstraintValidator<EndTimeAfterStartTime, ContestRequest> {

    @Override
    public boolean isValid(ContestRequest request, ConstraintValidatorContext context) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return true;
        }
        return request.getEndTime().isAfter(request.getStartTime());
    }

}
