package io.github.tapo.c210.application;

/** Active local recording session. */
public interface RecordingSession {
    void stop() throws CameraControlException;
}
