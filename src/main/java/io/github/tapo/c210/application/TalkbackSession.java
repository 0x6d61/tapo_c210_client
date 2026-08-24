package io.github.tapo.c210.application;

/** Active two-way audio session. */
public interface TalkbackSession {
    void stop() throws CameraControlException;
}
