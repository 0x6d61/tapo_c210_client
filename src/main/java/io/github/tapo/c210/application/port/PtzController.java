package io.github.tapo.c210.application.port;

import io.github.tapo.c210.application.CameraControlException;
import io.github.tapo.c210.domain.PtzCommand;

/** Adapter port for ONVIF PTZ control. */
public interface PtzController {
    void move(PtzCommand command) throws CameraControlException;

    void stop() throws CameraControlException;
}
