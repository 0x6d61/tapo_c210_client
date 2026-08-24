package io.github.tapo.c210.streaming;

import io.github.tapo.c210.application.CameraControlException;
import io.github.tapo.c210.application.ConnectedCamera;
import io.github.tapo.c210.application.RecordingSession;
import io.github.tapo.c210.application.port.RecordingEngine;
import java.nio.file.Path;
import java.util.Objects;

/** RecordingEngine adapter backed by a second VLCJ media player. */
public final class VlcjRecordingEngine implements RecordingEngine {
    private final VlcjRtspConnector connector;

    public VlcjRecordingEngine(VlcjRtspConnector connector) {
        this.connector = Objects.requireNonNull(connector, "connector must not be null");
    }

    @Override
    public RecordingSession start(ConnectedCamera camera, Path output)
            throws CameraControlException {
        return connector.startRecording(camera, output);
    }
}
