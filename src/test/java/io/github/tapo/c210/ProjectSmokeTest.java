package io.github.tapo.c210;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProjectSmokeTest {
    @Test
    void runsOnTheSupportedJavaVersion() {
        assertTrue(
                Runtime.version().feature() >= 21,
                "This project requires Java 21 or newer");
    }
}
