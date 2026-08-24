package io.github.tapo.c210.streaming;

import io.github.tapo.c210.application.CameraConnectionException;
import io.github.tapo.c210.application.ConnectedCamera;
import io.github.tapo.c210.application.RtspConnectionRequest;
import io.github.tapo.c210.application.port.RtspConnector;
import java.util.Objects;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

/** Opens an RTSP stream with VLCJ/libVLC without putting credentials in the MRL. */
public final class VlcjRtspConnector implements RtspConnector, AutoCloseable {
    private final MediaPlayerFactory factory;
    private boolean closed;

    public VlcjRtspConnector() throws CameraConnectionException {
        try {
            factory = new MediaPlayerFactory();
        } catch (UnsatisfiedLinkError error) {
            throw new CameraConnectionException(
                    "LibVLC native libraries could not be loaded", error);
        }
    }

    @Override
    public ConnectedCamera connect(RtspConnectionRequest request)
            throws CameraConnectionException {
        Objects.requireNonNull(request, "request must not be null");
        if (closed) {
            throw new CameraConnectionException("RTSP connector is already closed");
        }

        MediaPlayer player = null;
        try {
            player = factory.mediaPlayers().newMediaPlayer();
            var options = VlcjRtspOptions.from(request);
            if (!player.media().play(options.mediaResource(), options.asVlcjOptions())) {
                player.release();
                throw new CameraConnectionException("LibVLC rejected the RTSP stream");
            }
            return new VlcjRtspSession(player);
        } catch (CameraConnectionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (player != null) {
                player.release();
            }
            throw new CameraConnectionException("Could not start the RTSP stream", exception);
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            factory.release();
        }
    }

    private static final class VlcjRtspSession implements ConnectedCamera {
        private final MediaPlayer player;
        private boolean closed;

        private VlcjRtspSession(MediaPlayer player) {
            this.player = player;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                player.controls().stop();
                player.release();
            }
        }
    }
}
