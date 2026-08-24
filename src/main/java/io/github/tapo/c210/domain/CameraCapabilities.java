package io.github.tapo.c210.domain;

/** Features confirmed as available by the connected camera. */
public record CameraCapabilities(
        boolean ptz,
        boolean localRecording,
        boolean cameraStorageRecording,
        boolean motionEvents,
        boolean talkback) {

    public static CameraCapabilities none() {
        return new CameraCapabilities(false, false, false, false, false);
    }

    public static CameraCapabilities ptzOnly() {
        return new CameraCapabilities(true, false, false, false, false);
    }

    public static CameraCapabilities recordingOnly() {
        return new CameraCapabilities(false, true, false, false, false);
    }

    public static CameraCapabilities motionOnly() {
        return new CameraCapabilities(false, false, false, true, false);
    }

    public static CameraCapabilities talkbackOnly() {
        return new CameraCapabilities(false, false, false, false, true);
    }
}
