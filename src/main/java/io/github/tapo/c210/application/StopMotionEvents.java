package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.MotionEventSubscription;
import java.util.Objects;

/** Stops a motion-event subscription. */
public final class StopMotionEvents {
    public void execute(MotionEventSubscription subscription) throws CameraControlException {
        Objects.requireNonNull(subscription, "subscription must not be null").close();
    }
}
