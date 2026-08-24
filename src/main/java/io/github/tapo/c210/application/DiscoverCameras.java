package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.CameraDiscovery;
import io.github.tapo.c210.domain.CameraDevice;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Application use case for finding cameras on the local network. */
public final class DiscoverCameras {
    private final CameraDiscovery discovery;

    public DiscoverCameras(CameraDiscovery discovery) {
        this.discovery = Objects.requireNonNull(discovery, "discovery must not be null");
    }

    public List<CameraDevice> execute(Duration timeout, BooleanSupplier cancelled)
            throws CameraDiscoveryException {
        return discovery.discover(timeout, cancelled);
    }
}
