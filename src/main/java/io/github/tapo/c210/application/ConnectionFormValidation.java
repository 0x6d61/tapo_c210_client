package io.github.tapo.c210.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Result of validating raw connection-form values. */
public record ConnectionFormValidation(
        List<String> errors, Optional<ValidatedConnectionForm> value) {
    public ConnectionFormValidation {
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
        value = Objects.requireNonNull(value, "value must not be null");
        if (errors.isEmpty() == value.isEmpty()) {
            throw new IllegalArgumentException("errors and value must describe the same validity");
        }
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
