package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.TalkbackService;
import io.github.tapo.c210.domain.CameraCapabilities;
import java.util.Objects;

/** Starts two-way audio only when the camera advertises talkback support. */
public final class StartTalkback {
    private final CameraCapabilities capabilities;
    private final TalkbackService service;

    public StartTalkback(CameraCapabilities capabilities, TalkbackService service) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities must not be null");
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    public TalkbackSession execute() throws CameraControlException {
        if (!capabilities.talkback()) {
            throw new UnsupportedCameraFeatureException("talkback");
        }
        return Objects.requireNonNull(service.start(), "service returned null session");
    }
}
