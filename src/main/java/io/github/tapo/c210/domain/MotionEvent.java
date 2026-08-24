package io.github.tapo.c210.domain;

import java.time.Instant;
import java.util.Objects;

/** A normalized motion or detection event delivered to the UI. */
public record MotionEvent(Instant occurredAt, String type, String source) {
    public MotionEvent {
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requireNonBlank(type, "type");
        requireNonBlank(source, "source");
    }

    private static void requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
