package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.CameraCapabilityProvider;
import io.github.tapo.c210.domain.CameraCapabilities;
import java.util.Objects;

/** Loads feature support after the RTSP session has been opened. */
public final class LoadCapabilities {
    private final CameraCapabilityProvider provider;

    public LoadCapabilities(CameraCapabilityProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
    }

    public CameraCapabilities execute() throws CameraControlException {
        return Objects.requireNonNull(provider.load(), "provider returned null capabilities");
    }
}
