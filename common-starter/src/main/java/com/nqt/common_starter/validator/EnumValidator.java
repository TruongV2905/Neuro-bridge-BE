/* EMOB-2025 */
package com.nqt.common_starter.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {EnumValidatorClass.class})
public @interface EnumValidator {

  Class<? extends Enum<?>> enumClass();

  String message() default "Enum is not match";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
