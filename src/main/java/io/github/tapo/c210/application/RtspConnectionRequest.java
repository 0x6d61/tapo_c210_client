package io.github.tapo.c210.application;

import io.github.tapo.c210.domain.RtspEndpoint;
import java.util.Objects;

/** Separates an RTSP endpoint from the credentials needed by the adapter. */
public record RtspConnectionRequest(RtspEndpoint endpoint, CameraCredentials credentials) {
    public RtspConnectionRequest {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        Objects.requireNonNull(credentials, "credentials must not be null");
    }
}
