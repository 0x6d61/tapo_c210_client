package io.github.tapo.c210.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tapo.c210.domain.StreamQuality;
import org.junit.jupiter.api.Test;

class ConnectionFormValidatorTest {
    private final ConnectionFormValidator validator = new ConnectionFormValidator();

    @Test
    void acceptsAnIpv4AddressAndPreservesTheSelectedStream() {
        var result = validator.validate(new ConnectionFormInput(
                " 192.168.1.20 ", "2020", "554", "camera-user", "camera-password",
                StreamQuality.LOW, true));

        assertTrue(result.isValid());
        assertEquals("192.168.1.20", result.value().orElseThrow().host());
        assertEquals(2020, result.value().orElseThrow().onvifPort());
        assertEquals(554, result.value().orElseThrow().rtspPort());
        assertEquals(StreamQuality.LOW, result.value().orElseThrow().streamQuality());
        assertTrue(result.value().orElseThrow().remember());
    }

    @Test
    void reportsInvalidHostAndPortsWithoutExposingThePassword() {
        var result = validator.validate(new ConnectionFormInput(
                "not-an-ip", "zero", "70000", "", "secret-password", StreamQuality.HIGH, false));

        assertFalse(result.isValid());
        assertEquals(4, result.errors().size());
        assertTrue(result.errors().stream().noneMatch(error -> error.contains("secret-password")));
    }

    @Test
    void rejectsBlankPasswordBeforeAnyConnectionIsAttempted() {
        var result = validator.validate(new ConnectionFormInput(
                "192.168.1.20", "2020", "554", "camera-user", " ", StreamQuality.HIGH, false));

        assertFalse(result.isValid());
        assertEquals(1, result.errors().size());
    }
}
