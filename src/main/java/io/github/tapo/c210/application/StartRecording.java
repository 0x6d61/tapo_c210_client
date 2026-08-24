package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.RecordingEngine;
import io.github.tapo.c210.domain.CameraCapabilities;
import java.nio.file.Path;
import java.util.Objects;

/** Starts local recording only when the capability is available. */
public final class StartRecording {
    private final CameraCapabilities capabilities;
    private final RecordingEngine engine;

    public StartRecording(CameraCapabilities capabilities, RecordingEngine engine) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    public RecordingSession execute(ConnectedCamera camera, Path output)
            throws CameraControlException {
        Objects.requireNonNull(camera, "camera must not be null");
        Objects.requireNonNull(output, "output must not be null");
        if (!capabilities.localRecording()) {
            throw new UnsupportedCameraFeatureException("local recording");
        }
        return Objects.requireNonNull(engine.start(camera, output), "engine returned null session");
    }
}
