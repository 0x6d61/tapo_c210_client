package io.github.tapo.c210.application.port;

import io.github.tapo.c210.application.CameraControlException;
import io.github.tapo.c210.domain.MotionEvent;
import java.util.function.Consumer;

/** Adapter port for ONVIF detection-event subscriptions. */
public interface MotionEventSource {
    MotionEventSubscription subscribe(Consumer<MotionEvent> listener)
            throws CameraControlException;
}
