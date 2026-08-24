package io.github.tapo.c210.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StreamQualityTest {
    @Test
    void highQualityUsesStreamOne() {
        assertEquals("/stream1", StreamQuality.HIGH.path());
    }

    @Test
    void lowQualityUsesStreamTwo() {
        assertEquals("/stream2", StreamQuality.LOW.path());
    }
}
