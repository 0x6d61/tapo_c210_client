package io.github.tapo.c210.domain;

/** Selects one of the camera's RTSP video streams. */
public enum StreamQuality {
    HIGH("/stream1"),
    LOW("/stream2");

    private final String path;

    StreamQuality(String path) {
        this.path = path;
    }

    /** Returns the RTSP path exposed by the camera for this quality. */
    public String path() {
        return path;
    }
}
