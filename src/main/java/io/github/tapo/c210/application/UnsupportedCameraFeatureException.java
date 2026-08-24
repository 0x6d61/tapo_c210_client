package io.github.tapo.c210.application;

/** Indicates that the camera did not advertise a requested feature. */
public final class UnsupportedCameraFeatureException extends CameraControlException {
    public UnsupportedCameraFeatureException(String feature) {
        super("Camera does not support " + feature);
    }
}
