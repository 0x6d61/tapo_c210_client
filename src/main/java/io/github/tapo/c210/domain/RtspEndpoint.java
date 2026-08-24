package io.github.tapo.c210.domain;

import java.net.URI;
import java.util.Objects;

/** Describes an RTSP endpoint without embedding user credentials. */
public record RtspEndpoint(String host, int port, StreamQuality quality) {
    public RtspEndpoint {
        Objects.requireNonNull(host, "host must not be null");
        Objects.requireNonNull(quality, "quality must not be null");

        host = host.trim();

        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    /** Builds the endpoint URI without user information or query parameters. */
    public URI toUri() {
        return URI.create("rtsp://%s:%d%s".formatted(host, port, quality.path()));
    }
}
