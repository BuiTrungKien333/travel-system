package com.travel.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = Utf8ByteLengthValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Utf8ByteLength {
    String message() default "Password must not exceed 72 UTF-8 bytes";

    int max() default 72;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
