package io.github.tapo.c210.application;

/** Indicates that a camera discovery operation could not complete. */
public class CameraDiscoveryException extends Exception {
    public CameraDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
