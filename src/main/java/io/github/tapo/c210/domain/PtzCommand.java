package io.github.tapo.c210.domain;

import java.time.Duration;
import java.util.Objects;

/** A bounded PTZ movement request. */
public record PtzCommand(PtzDirection direction, double speed, Duration duration) {
    public PtzCommand {
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        if (!Double.isFinite(speed) || speed <= 0.0 || speed > 1.0) {
            throw new IllegalArgumentException("speed must be greater than 0 and at most 1");
        }
        if (duration.isZero() || duration.isNegative() || duration.compareTo(Duration.ofSeconds(5)) > 0) {
            throw new IllegalArgumentException("duration must be between 1ms and 5s");
        }
    }
}
