package io.github.tapo.c210.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.tapo.c210.application.port.CameraDiscovery;
import io.github.tapo.c210.domain.CameraDevice;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiscoverCamerasTest {
    @Test
    void delegatesDiscoveryWithTheRequestedTimeoutAndCancellation() throws Exception {
        var device = new CameraDevice(
                "urn:uuid:camera-1",
                "192.168.1.20",
                2020,
                554,
                URI.create("http://192.168.1.20:2020/onvif/device_service"),
                "TP-Link",
                "Tapo C210",
                "C210");
        var discovery = new StubDiscovery(List.of(device));
        var useCase = new DiscoverCameras(discovery);

        assertEquals(List.of(device), useCase.execute(Duration.ofSeconds(3), () -> false));
        assertEquals(Duration.ofSeconds(3), discovery.timeout);
    }

    private static final class StubDiscovery implements CameraDiscovery {
        private final List<CameraDevice> devices;
        private Duration timeout;

        private StubDiscovery(List<CameraDevice> devices) {
            this.devices = devices;
        }

        @Override
        public List<CameraDevice> discover(Duration timeout, java.util.function.BooleanSupplier cancelled) {
            this.timeout = timeout;
            return devices;
        }
    }
}
