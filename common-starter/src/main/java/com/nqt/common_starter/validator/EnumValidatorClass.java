/* EMOB-2025 */
package com.nqt.common_starter.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class EnumValidatorClass implements ConstraintValidator<EnumValidator, Object> {

  private Class<? extends Enum<?>> enumClass;
  private String message;

  @Override
  public void initialize(EnumValidator constraintAnnotation) {
    this.enumClass = constraintAnnotation.enumClass();
    this.message = constraintAnnotation.message();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value == null) return true; // Cho phép null (nếu muốn bắt buộc => thêm @NotNull riêng)

    String stringValue = String.valueOf(value);

    boolean matched =
        Arrays.stream(enumClass.getEnumConstants())
            .anyMatch(e -> e.name().equalsIgnoreCase(stringValue));

    if (matched) return true;

    // Nếu không khớp, tạo message chứa danh sách enum
    String allowedValues =
        String.join(", ", Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toList());

    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate(message + ". Allowed values: [" + allowedValues + "]")
        .addConstraintViolation();

    return false;
  }
}
