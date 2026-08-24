package io.github.tapo.c210.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tapo.c210.application.port.MotionEventSource;
import io.github.tapo.c210.application.port.MotionEventSubscription;
import io.github.tapo.c210.application.port.PtzController;
import io.github.tapo.c210.application.port.RecordingEngine;
import io.github.tapo.c210.application.port.TalkbackService;
import io.github.tapo.c210.domain.CameraCapabilities;
import io.github.tapo.c210.domain.MotionEvent;
import io.github.tapo.c210.domain.PtzCommand;
import io.github.tapo.c210.domain.PtzDirection;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class CameraControlUseCasesTest {
    @Test
    void loadsCapabilitiesFromTheProvider() throws Exception {
        var expected = new CameraCapabilities(true, true, false, true, false);

        var result = new LoadCapabilities(() -> expected).execute();

        assertEquals(expected, result);
    }

    @Test
    void movesTheCameraWhenPtzIsSupported() throws Exception {
        var controller = new CapturingPtzController();
        var command = new PtzCommand(PtzDirection.PAN_LEFT, 0.5, Duration.ofMillis(300));

        new MoveCamera(CameraCapabilities.ptzOnly(), controller).execute(command);

        assertEquals(command, controller.command);
    }

    @Test
    void refusesPtzWhenTheCameraDoesNotAdvertiseIt() {
        var controller = new CapturingPtzController();

        assertThrows(
                UnsupportedCameraFeatureException.class,
                () -> new MoveCamera(CameraCapabilities.none(), controller)
                        .execute(new PtzCommand(PtzDirection.PAN_LEFT, 0.5, Duration.ofMillis(300))));
        assertTrue(controller.command == null);
    }

    @Test
    void startsAndStopsLocalRecordingWhenSupported() throws Exception {
        var engine = new CapturingRecordingEngine();
        var session = new TestRecordingSession();
        engine.session = session;
        var camera = new TestConnectedCamera();
        var output = Path.of("recordings", "c210.mp4");

        var result = new StartRecording(CameraCapabilities.recordingOnly(), engine)
                .execute(camera, output);
        new StopRecording().execute(result);

        assertEquals(camera, engine.camera);
        assertEquals(output, engine.output);
        assertTrue(session.stopped);
    }

    @Test
    void subscribesToMotionEventsWhenSupported() throws Exception {
        var source = new CapturingMotionEventSource();
        var listener = (Consumer<MotionEvent>) event -> { };

        var subscription = new SubscribeMotionEvents(CameraCapabilities.motionOnly(), source)
                .execute(listener);
        new StopMotionEvents().execute(subscription);

        assertEquals(listener, source.listener);
        assertTrue(source.subscription.closed);
    }

    @Test
    void startsAndStopsTalkbackWhenSupported() throws Exception {
        var service = new CapturingTalkbackService();
        var session = new TestTalkbackSession();
        service.session = session;

        var result = new StartTalkback(CameraCapabilities.talkbackOnly(), service).execute();
        new StopTalkback().execute(result);

        assertTrue(service.started);
        assertTrue(session.stopped);
    }

    private static final class CapturingPtzController implements PtzController {
        private PtzCommand command;

        @Override
        public void move(PtzCommand command) {
            this.command = command;
        }

        @Override
        public void stop() {
        }
    }

    private static final class CapturingRecordingEngine implements RecordingEngine {
        private ConnectedCamera camera;
        private Path output;
        private RecordingSession session;

        @Override
        public RecordingSession start(ConnectedCamera camera, Path output) {
            this.camera = camera;
            this.output = output;
            return session;
        }
    }

    private static final class CapturingMotionEventSource implements MotionEventSource {
        private Consumer<MotionEvent> listener;
        private final TestMotionEventSubscription subscription = new TestMotionEventSubscription();

        @Override
        public MotionEventSubscription subscribe(Consumer<MotionEvent> listener) {
            this.listener = listener;
            return subscription;
        }
    }

    private static final class CapturingTalkbackService implements TalkbackService {
        private boolean started;
        private TalkbackSession session;

        @Override
        public TalkbackSession start() {
            started = true;
            return session;
        }
    }

    private static final class TestConnectedCamera implements ConnectedCamera {
        @Override
        public void close() {
        }
    }

    private static final class TestRecordingSession implements RecordingSession {
        private boolean stopped;

        @Override
        public void stop() {
            stopped = true;
        }
    }

    private static final class TestMotionEventSubscription implements MotionEventSubscription {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class TestTalkbackSession implements TalkbackSession {
        private boolean stopped;

        @Override
        public void stop() {
            stopped = true;
        }
    }
}
