package io.github.tapo.c210.domain;

import java.time.Instant;
import java.util.Objects;

/** A saved camera connection without its password. */
public record CameraProfile(
        String id,
        String displayName,
        String deviceId,
        String host,
        int onvifPort,
        int rtspPort,
        String username,
        StreamQuality streamQuality,
        Instant lastUsedAt) {

    public CameraProfile {
        requireNonBlank(id, "id");
        requireNonBlank(displayName, "displayName");
        requireNonBlank(host, "host");
        requireNonBlank(username, "username");
        Objects.requireNonNull(streamQuality, "streamQuality must not be null");
        Objects.requireNonNull(lastUsedAt, "lastUsedAt must not be null");
        validatePort(onvifPort, "onvifPort");
        validatePort(rtspPort, "rtspPort");
    }

    private static void requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static void validatePort(int port, String fieldName) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and 65535");
        }
    }
}
