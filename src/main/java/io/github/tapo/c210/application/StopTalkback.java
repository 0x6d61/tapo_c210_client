package io.github.tapo.c210.application;

import java.util.Objects;

/** Stops two-way audio. */
public final class StopTalkback {
    public void execute(TalkbackSession session) throws CameraControlException {
        Objects.requireNonNull(session, "session must not be null").stop();
    }
}
