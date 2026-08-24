package io.github.tapo.c210.application.port;

import io.github.tapo.c210.application.CameraDiscoveryException;
import io.github.tapo.c210.domain.CameraDevice;
import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Port for discovering cameras on the local network. */
public interface CameraDiscovery {
    List<CameraDevice> discover(Duration timeout, BooleanSupplier cancelled)
            throws CameraDiscoveryException;
}
