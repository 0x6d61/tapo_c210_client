package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.PtzController;
import io.github.tapo.c210.domain.CameraCapabilities;
import io.github.tapo.c210.domain.PtzCommand;
import java.util.Objects;

/** Performs a bounded PTZ movement only when the camera advertises PTZ support. */
public final class MoveCamera {
    private final CameraCapabilities capabilities;
    private final PtzController controller;

    public MoveCamera(CameraCapabilities capabilities, PtzController controller) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities must not be null");
        this.controller = Objects.requireNonNull(controller, "controller must not be null");
    }

    public void execute(PtzCommand command)
            throws CameraControlException, UnsupportedCameraFeatureException {
        Objects.requireNonNull(command, "command must not be null");
        if (!capabilities.ptz()) {
            throw new UnsupportedCameraFeatureException("PTZ");
        }
        controller.move(command);
    }
}
