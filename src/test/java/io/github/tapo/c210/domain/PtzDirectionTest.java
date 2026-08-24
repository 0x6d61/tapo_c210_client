package io.github.tapo.c210.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class PtzDirectionTest {
    @Test
    void containsOnlyC210PanAndTiltDirections() {
        assertArrayEquals(
                new PtzDirection[] {
                    PtzDirection.PAN_LEFT,
                    PtzDirection.PAN_RIGHT,
                    PtzDirection.TILT_UP,
                    PtzDirection.TILT_DOWN
                },
                PtzDirection.values());
    }
}
