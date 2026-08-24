package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.PtzController;
import io.github.tapo.c210.domain.CameraCapabilities;
import java.util.Objects;

/** Stops an active PTZ movement. */
public final class StopCameraMovement {
    private final CameraCapabilities capabilities;
    private final PtzController controller;

    public StopCameraMovement(CameraCapabilities capabilities, PtzController controller) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities must not be null");
        this.controller = Objects.requireNonNull(controller, "controller must not be null");
    }

    public void execute() throws CameraControlException {
        if (!capabilities.ptz()) {
            throw new UnsupportedCameraFeatureException("PTZ");
        }
        controller.stop();
    }
}
