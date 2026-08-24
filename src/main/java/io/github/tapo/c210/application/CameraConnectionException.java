package io.github.tapo.c210.application;

/** Indicates that a camera connection could not be opened. */
public class CameraConnectionException extends Exception {
    public CameraConnectionException(String message) {
        super(message);
    }

    public CameraConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
