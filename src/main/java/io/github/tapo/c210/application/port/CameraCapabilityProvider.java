package io.github.tapo.c210.application.port;

import io.github.tapo.c210.application.CameraControlException;
import io.github.tapo.c210.domain.CameraCapabilities;

/** Adapter port that probes the connected camera and reports usable features. */
@FunctionalInterface
public interface CameraCapabilityProvider {
    CameraCapabilities load() throws CameraControlException;
}
