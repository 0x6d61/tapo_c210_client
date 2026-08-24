package io.github.tapo.c210.application.port;

import io.github.tapo.c210.application.CameraControlException;

/** Handle for an active camera-event subscription. */
public interface MotionEventSubscription {
    void close() throws CameraControlException;
}
