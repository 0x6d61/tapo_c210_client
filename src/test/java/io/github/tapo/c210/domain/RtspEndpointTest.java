package io.github.tapo.c210.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

class RtspEndpointTest {
    @Test
    void buildsAnRtspUriWithoutCredentials() {
        var endpoint = new RtspEndpoint("192.168.1.20", 554, StreamQuality.HIGH);

        assertEquals(URI.create("rtsp://192.168.1.20:554/stream1"), endpoint.toUri());
    }

    @Test
    void rejectsAnInvalidPort() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RtspEndpoint("192.168.1.20", 0, StreamQuality.HIGH));
    }

    @Test
    void rejectsAnEmptyHost() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RtspEndpoint(" ", 554, StreamQuality.HIGH));
    }
}
