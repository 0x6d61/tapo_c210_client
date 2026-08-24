package io.github.tapo.c210.streaming;

import io.github.tapo.c210.application.RtspConnectionRequest;
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

    @Override
    public String toString() {
        return "VlcjRtspOptions[mediaResource='%s', username='%s', password=<redacted>]"
                .formatted(mediaResource, username);
    }
}
