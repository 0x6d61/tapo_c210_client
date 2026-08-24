package io.github.tapo.c210.application;

/** Indicates that the native VLC runtime required by VLCJ is unavailable. */
public final class LibVlcUnavailableException extends CameraConnectionException {
    public LibVlcUnavailableException(Throwable cause) {
        super("RTSP playback requires 64-bit VLC/libVLC to be installed and loadable", cause);
    }
}
