package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.MotionEventSource;
import io.github.tapo.c210.application.port.MotionEventSubscription;
import io.github.tapo.c210.domain.CameraCapabilities;
import io.github.tapo.c210.domain.MotionEvent;
import java.util.Objects;
import java.util.function.Consumer;

/** Subscribes to camera events only when motion events are available. */
public final class SubscribeMotionEvents {
    private final CameraCapabilities capabilities;
    private final MotionEventSource source;

    public SubscribeMotionEvents(CameraCapabilities capabilities, MotionEventSource source) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities must not be null");
        this.source = Objects.requireNonNull(source, "source must not be null");
    }

    public MotionEventSubscription execute(Consumer<MotionEvent> listener)
            throws CameraControlException {
        Objects.requireNonNull(listener, "listener must not be null");
        if (!capabilities.motionEvents()) {
            throw new UnsupportedCameraFeatureException("motion events");
        }
        return Objects.requireNonNull(source.subscribe(listener), "source returned null subscription");
    }
}
