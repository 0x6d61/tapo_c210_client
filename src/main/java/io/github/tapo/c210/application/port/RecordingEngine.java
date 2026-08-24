package io.github.tapo.c210.application.port;

import io.github.tapo.c210.application.CameraControlException;
import io.github.tapo.c210.application.ConnectedCamera;
import io.github.tapo.c210.application.RecordingSession;
import java.nio.file.Path;

/** Adapter port for recording the active RTSP stream to local storage. */
public interface RecordingEngine {
    RecordingSession start(ConnectedCamera camera, Path output) throws CameraControlException;
}
