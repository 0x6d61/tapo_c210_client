package io.github.tapo.c210.application.port;

import io.github.tapo.c210.application.CameraConnectionException;
import io.github.tapo.c210.application.ConnectedCamera;
import io.github.tapo.c210.application.RtspConnectionRequest;

/** Adapter port for an RTSP library such as VLCJ/libVLC. */
@FunctionalInterface
public interface RtspConnector {
    ConnectedCamera connect(RtspConnectionRequest request) throws CameraConnectionException;
}
