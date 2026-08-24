package io.github.tapo.c210.application;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LibVlcUnavailableExceptionTest {
    @Test
    void explainsThatA64BitVlcInstallationIsRequired() {
        var cause = new UnsatisfiedLinkError("libvlc.dll not found");

        var exception = new LibVlcUnavailableException(cause);

        assertTrue(exception.getMessage().contains("64-bit VLC"));
        assertSame(cause, exception.getCause());
    }
}
