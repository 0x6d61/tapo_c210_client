package io.github.tapo.c210.streaming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tapo.c210.application.CameraCredentials;
import io.github.tapo.c210.application.RtspConnectionRequest;
import io.github.tapo.c210.domain.RtspEndpoint;
import io.github.tapo.c210.domain.StreamQuality;
import org.junit.jupiter.api.Test;

class VlcjRtspOptionsTest {
    @Test
    void usesStreamOneForTheHighQualityRequestWithoutEmbeddingCredentialsInTheUri() {
        var options = VlcjRtspOptions.from(new RtspConnectionRequest(
                new RtspEndpoint("192.168.1.20", 554, StreamQuality.HIGH),
                new CameraCredentials("camera-user", "camera-password")));

        assertEquals("rtsp://192.168.1.20:554/stream1", options.mediaResource());
        assertTrue(options.asVlcjOptions()[0].contains("camera-user"));
        assertTrue(options.asVlcjOptions()[1].contains("camera-password"));
        assertFalse(options.mediaResource().contains("camera-password"));
        assertFalse(options.toString().contains("camera-password"));
    }

    @Test
    void usesStreamTwoWhenLowQualityIsSelected() {
        var options = VlcjRtspOptions.from(new RtspConnectionRequest(
                new RtspEndpoint("192.168.1.20", 554, StreamQuality.LOW),
                new CameraCredentials("camera-user", "camera-password")));

        assertEquals("rtsp://192.168.1.20:554/stream2", options.mediaResource());
    }
}
