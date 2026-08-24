package io.github.tapo.c210.application;

/** Indicates that an operation against a connected camera could not be completed. */
public class CameraControlException extends Exception {
    public CameraControlException(String message) {
        super(message);
    }

    public CameraControlException(String message, Throwable cause) {
        super(message, cause);
    }
}
