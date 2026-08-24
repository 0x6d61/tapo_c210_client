package io.github.tapo.c210.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RecordingFileNameTest {
    @Test
    void createsAnMp4FileNameSafeForWindowsPaths() {
        assertEquals(
                "c210-2026-08-24T08-00-00Z.mp4",
                RecordingFileName.create(Instant.parse("2026-08-24T08:00:00Z")));
    }
}
