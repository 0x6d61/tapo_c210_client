package io.github.tapo.c210.application;

import io.github.tapo.c210.domain.StreamQuality;
import java.util.Objects;

/** Normalized connection-form values safe to pass to an application use case. */
public final class ValidatedConnectionForm {
    private final String host;
    private final int onvifPort;
    private final int rtspPort;
    private final String username;
    private final String password;
    private final StreamQuality streamQuality;
    private final boolean remember;

    public ValidatedConnectionForm(
            String host,
            int onvifPort,
            int rtspPort,
            String username,
            String password,
            StreamQuality streamQuality,
            boolean remember) {
        this.host = Objects.requireNonNull(host, "host must not be null");
        this.onvifPort = onvifPort;
        this.rtspPort = rtspPort;
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.password = Objects.requireNonNull(password, "password must not be null");
        this.streamQuality = Objects.requireNonNull(streamQuality, "streamQuality must not be null");
        this.remember = remember;
    }

    public String host() {
        return host;
    }

    public int onvifPort() {
        return onvifPort;
    }

    public int rtspPort() {
        return rtspPort;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public StreamQuality streamQuality() {
        return streamQuality;
    }

    public boolean remember() {
        return remember;
    }

    @Override
    public String toString() {
        return "ValidatedConnectionForm[host='%s', onvifPort=%d, rtspPort=%d, username='%s', "
                + "password=<redacted>, streamQuality=%s, remember=%s]"
                .formatted(host, onvifPort, rtspPort, username, streamQuality, remember);
    }
}
