package io.github.tapo.c210.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tapo.c210.application.port.RtspConnector;
import io.github.tapo.c210.domain.StreamQuality;
import org.junit.jupiter.api.Test;

class SwitchStreamQualityTest {
    @Test
    void connectsTheRequestedQualityBeforeClosingTheCurrentSession() throws Exception {
        var current = new TestSession();
        var replacement = new TestSession();
        var connector = new CapturingConnector(replacement);

        var result = new SwitchStreamQuality(connector).execute(
                current,
                "192.168.1.20",
                554,
                new CameraCredentials("camera-user", "camera-password"),
                StreamQuality.LOW);

        assertSame(replacement, result);
        assertTrue(current.closed);
        assertEquals("192.168.1.20", connector.request.endpoint().host());
        assertEquals(StreamQuality.LOW, connector.request.endpoint().quality());
    }

    @Test
    void keepsTheCurrentSessionWhenTheReplacementCannotOpen() {
        var current = new TestSession();
        var connector = new CapturingConnector(null);
        connector.failure = new CameraConnectionException("connection failed");

        assertThrows(
                CameraConnectionException.class,
                () -> new SwitchStreamQuality(connector).execute(
                        current,
                        "192.168.1.20",
                        554,
                        new CameraCredentials("camera-user", "camera-password"),
                        StreamQuality.LOW));

        assertFalse(current.closed);
    }

    private static final class CapturingConnector implements RtspConnector {
        private final ConnectedCamera session;
        private RtspConnectionRequest request;
        private CameraConnectionException failure;

        private CapturingConnector(ConnectedCamera session) {
            this.session = session;
        }

        @Override
        public ConnectedCamera connect(RtspConnectionRequest request)
                throws CameraConnectionException {
            this.request = request;
            if (failure != null) {
                throw failure;
            }
            return session;
        }
    }

    private static final class TestSession implements ConnectedCamera {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }
}
