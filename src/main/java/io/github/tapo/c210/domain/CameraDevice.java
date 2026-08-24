package io.github.tapo.c210.domain;

import java.net.URI;
import java.util.Objects;

/** Camera metadata obtained from ONVIF discovery. */
public record CameraDevice(
        String deviceId,
        String host,
        int onvifPort,
        int rtspPort,
        URI serviceUrl,
        String manufacturer,
        String model,
        String hardwareVersion) {

    public CameraDevice {
        requireNonBlank(deviceId, "deviceId");
        requireNonBlank(host, "host");
        Objects.requireNonNull(serviceUrl, "serviceUrl must not be null");
        if (!"http".equalsIgnoreCase(serviceUrl.getScheme())
                && !"https".equalsIgnoreCase(serviceUrl.getScheme())) {
            throw new IllegalArgumentException("serviceUrl must use HTTP or HTTPS");
        }
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
