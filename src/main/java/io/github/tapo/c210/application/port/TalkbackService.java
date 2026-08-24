package io.github.tapo.c210.application.port;

import io.github.tapo.c210.application.CameraControlException;
import io.github.tapo.c210.application.TalkbackSession;

/** Adapter port for camera-specific two-way audio. */
public interface TalkbackService {
    TalkbackSession start() throws CameraControlException;
}
