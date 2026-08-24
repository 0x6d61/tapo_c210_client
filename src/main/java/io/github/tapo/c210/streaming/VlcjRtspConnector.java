package io.github.tapo.c210.streaming;

import io.github.tapo.c210.application.CameraConnectionException;
import io.github.tapo.c210.application.ConnectedCamera;
import io.github.tapo.c210.application.LibVlcUnavailableException;
import io.github.tapo.c210.application.RtspConnectionRequest;
import io.github.tapo.c210.application.RecordingSession;
import io.github.tapo.c210.application.port.RtspConnector;
import io.github.tapo.c210.application.CameraControlException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.scene.image.ImageView;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;

/** Opens an RTSP stream with VLCJ/libVLC without putting credentials in the MRL. */
public final class VlcjRtspConnector implements RtspConnector, AutoCloseable {
    private final MediaPlayerFactory factory;
    private boolean closed;

    public VlcjRtspConnector() throws CameraConnectionException {
        try {
            factory = new MediaPlayerFactory();
        } catch (UnsatisfiedLinkError error) {
            throw new LibVlcUnavailableException(error);
        }
    }

    @Override
    public ConnectedCamera connect(RtspConnectionRequest request)
            throws CameraConnectionException {
        return connect(request, player -> { }, null);
    }

    /** Opens a stream and attaches its decoded video frames to a JavaFX image view. */
    public ConnectedCamera connect(RtspConnectionRequest request, ImageView imageView)
            throws CameraConnectionException {
        Objects.requireNonNull(imageView, "imageView must not be null");
        var videoSurface = new ImageViewVideoSurface(imageView);
        return connect(request, videoSurface::attach, videoSurface);
    }

    /** Starts a second VLCJ player that records the existing RTSP session to an MP4 file. */
    public RecordingSession startRecording(ConnectedCamera camera, Path output)
            throws CameraControlException {
        Objects.requireNonNull(camera, "camera must not be null");
        Objects.requireNonNull(output, "output must not be null");
        if (closed) {
            throw new CameraControlException("RTSP connector is already closed");
        }
        if (!(camera instanceof VlcjRtspSession session)) {
            throw new CameraControlException("Recording requires a VLCJ RTSP session");
        }
        var absoluteOutput = output.toAbsolutePath().normalize();
        createParentDirectory(absoluteOutput);
        MediaPlayer recorder = null;
        try {
            recorder = factory.mediaPlayers().newMediaPlayer();
            if (!recorder.media().play(
                    session.options.mediaResource(),
                    session.options.asRecordingVlcjOptions(absoluteOutput))) {
                recorder.release();
                throw new CameraControlException("LibVLC rejected the recording stream");
            }
            return new VlcjRecordingSession(recorder);
        } catch (CameraControlException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (recorder != null) {
                recorder.release();
            }
            throw new CameraControlException("Could not start local recording", exception);
        }
    }

    private ConnectedCamera connect(
            RtspConnectionRequest request,
            Consumer<MediaPlayer> videoSurfaceAttacher,
            ImageViewVideoSurface videoSurface)
            throws CameraConnectionException {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(videoSurfaceAttacher, "videoSurfaceAttacher must not be null");
        if (closed) {
            throw new CameraConnectionException("RTSP connector is already closed");
        }

        MediaPlayer player = null;
        try {
            player = factory.mediaPlayers().newMediaPlayer();
            var options = VlcjRtspOptions.from(request);
            videoSurfaceAttacher.accept(player);
            if (!player.media().play(options.mediaResource(), options.asVlcjOptions())) {
                player.release();
                throw new CameraConnectionException("LibVLC rejected the RTSP stream");
            }
            return new VlcjRtspSession(player, options, videoSurface);
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
        private final VlcjRtspOptions options;
        @SuppressWarnings("unused")
        private final ImageViewVideoSurface videoSurface;
        private boolean closed;

        private VlcjRtspSession(
                MediaPlayer player,
                VlcjRtspOptions options,
                ImageViewVideoSurface videoSurface) {
            this.player = player;
            this.options = options;
            this.videoSurface = videoSurface;
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

    private static final class VlcjRecordingSession implements RecordingSession {
        private final MediaPlayer player;
        private boolean stopped;

        private VlcjRecordingSession(MediaPlayer player) {
            this.player = player;
        }

        @Override
        public void stop() {
            if (!stopped) {
                stopped = true;
                player.controls().stop();
                player.release();
            }
        }
    }

    private static void createParentDirectory(Path output) throws CameraControlException {
        var parent = output.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new CameraControlException("Could not create the recording directory", exception);
        }
    }
}
