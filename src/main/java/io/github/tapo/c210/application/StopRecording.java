package io.github.tapo.c210.application;

import java.util.Objects;

/** Stops local recording. */
public final class StopRecording {
    public void execute(RecordingSession session) throws CameraControlException {
        Objects.requireNonNull(session, "session must not be null").stop();
    }
}
