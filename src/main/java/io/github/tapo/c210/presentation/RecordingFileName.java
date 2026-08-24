package io.github.tapo.c210.presentation;

import java.time.Instant;
import java.util.Objects;

final class RecordingFileName {
    private RecordingFileName() {
    }

    static String create(Instant timestamp) {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        return "c210-%s.mp4".formatted(timestamp.toString().replace(":", "-"));
    }
}
