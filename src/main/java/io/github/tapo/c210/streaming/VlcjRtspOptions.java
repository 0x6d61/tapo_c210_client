package io.github.tapo.c210.streaming;

import io.github.tapo.c210.application.RtspConnectionRequest;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/** Converts a credential-separated RTSP request into VLCJ media options. */
public final class VlcjRtspOptions {
    private final String mediaResource;
    private final String username;
    private final String password;

    private VlcjRtspOptions(String mediaResource, String username, String password) {
        this.mediaResource = mediaResource;
        this.username = username;
        this.password = password;
    }

    public static VlcjRtspOptions from(RtspConnectionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return new VlcjRtspOptions(
                request.endpoint().toUri().toString(),
                request.credentials().username(),
                request.credentials().password());
    }

    public String mediaResource() {
        return mediaResource;
    }

    String[] asVlcjOptions() {
        return new String[] {
            ":rtsp-user=" + username,
            ":rtsp-pwd=" + password,
            ":network-caching=300"
        };
    }

    String[] asRecordingVlcjOptions(Path output) {
        Objects.requireNonNull(output, "output must not be null");
        var base = asVlcjOptions();
        var recording = new String[] {
            ":sout=#transcode{vcodec=copy,acodec=none}:std{access=file,mux=mp4,dst=\"%s\"}".formatted(
                    output.toAbsolutePath().normalize()),
            ":sout-keep",
            ":vout=dummy"
        };
        var combined = Arrays.copyOf(base, base.length + recording.length);
        System.arraycopy(recording, 0, combined, base.length, recording.length);
        return combined;
    }

    @Override
    public String toString() {
        return "VlcjRtspOptions[mediaResource='%s', username='%s', password=<redacted>]"
                .formatted(mediaResource, username);
    }
}
