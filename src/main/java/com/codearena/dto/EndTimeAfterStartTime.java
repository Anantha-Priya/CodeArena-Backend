package com.codearena.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EndTimeAfterStartTimeValidator.class)
public @interface EndTimeAfterStartTime {

    String message() default "end_time must be after start_time";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
