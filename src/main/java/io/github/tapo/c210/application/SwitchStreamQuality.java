package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.RtspConnector;
import io.github.tapo.c210.domain.RtspEndpoint;
import io.github.tapo.c210.domain.StreamQuality;
import java.util.Objects;

/** Reconnects an RTSP stream at a different quality without dropping the current stream first. */
public final class SwitchStreamQuality {
    private final RtspConnector connector;

    public SwitchStreamQuality(RtspConnector connector) {
        this.connector = Objects.requireNonNull(connector, "connector must not be null");
    }

    public ConnectedCamera execute(
            ConnectedCamera current,
            String host,
            int rtspPort,
            CameraCredentials credentials,
            StreamQuality quality) throws CameraConnectionException {
        Objects.requireNonNull(current, "current must not be null");
        Objects.requireNonNull(host, "host must not be null");
        Objects.requireNonNull(credentials, "credentials must not be null");
        Objects.requireNonNull(quality, "quality must not be null");

        var replacement = connector.connect(new RtspConnectionRequest(
                new RtspEndpoint(host, rtspPort, quality), credentials));
        try {
            current.close();
        } catch (Exception exception) {
            try {
                replacement.close();
            } catch (Exception closeException) {
                exception.addSuppressed(closeException);
            }
            throw new CameraConnectionException(
                    "Could not close the current RTSP stream", exception);
        }
        return replacement;
    }
}
