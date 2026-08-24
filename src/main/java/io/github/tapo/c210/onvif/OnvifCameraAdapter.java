package io.github.tapo.c210.onvif;

import io.github.hyeonmo.client.OnvifClient;
import io.github.hyeonmo.models.OnvifDevice;
import io.github.hyeonmo.models.events.Event;
import io.github.hyeonmo.models.events.EventSession;
import io.github.tapo.c210.application.CameraControlException;
import io.github.tapo.c210.application.CameraCredentials;
import io.github.tapo.c210.application.port.CameraCapabilityProvider;
import io.github.tapo.c210.application.port.MotionEventSource;
import io.github.tapo.c210.application.port.MotionEventSubscription;
import io.github.tapo.c210.application.port.PtzController;
import io.github.tapo.c210.domain.CameraCapabilities;
import io.github.tapo.c210.domain.CameraDevice;
import io.github.tapo.c210.domain.MotionEvent;
import io.github.tapo.c210.domain.PtzCommand;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/** ONVIF adapter for capability probing, PTZ, and motion events. */
public final class OnvifCameraAdapter
        implements CameraCapabilityProvider, PtzController, MotionEventSource, AutoCloseable {
    private final OnvifDevice device;
    private final Duration timeout;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledStop;
    private boolean closed;

    private OnvifCameraAdapter(OnvifDevice device, Duration timeout) {
        this.device = device;
        this.timeout = timeout;
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "tapo-c210-ptz");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Connects to the ONVIF device and performs the library's initial service discovery. */
    public static OnvifCameraAdapter connect(
            CameraDevice camera, CameraCredentials credentials, Duration timeout)
            throws CameraControlException {
        Objects.requireNonNull(camera, "camera must not be null");
        Objects.requireNonNull(credentials, "credentials must not be null");
        validateTimeout(timeout);
        try {
            var device = OnvifClient.connect(camera.serviceUrl().toString())
                    .credentials(credentials.username(), credentials.password())
                    .buildAsync()
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return new OnvifCameraAdapter(device, timeout);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CameraControlException("ONVIF connection was interrupted", exception);
        } catch (ExecutionException | TimeoutException | RuntimeException exception) {
            throw new CameraControlException("Could not connect to the ONVIF service", exception);
        }
    }

    @Override
    public synchronized CameraCapabilities load() throws CameraControlException {
        ensureOpen();
        var capabilities = await(device.device().getCapabilities());
        return OnvifCapabilityMapper.toDomain(capabilities);
    }

    @Override
    public synchronized void move(PtzCommand command) throws CameraControlException {
        Objects.requireNonNull(command, "command must not be null");
        ensureOpen();
        cancelScheduledStop();
        await(device.ptz().move(OnvifPtzTypeMapper.toOnvif(command.direction())));
        scheduledStop = scheduler.schedule(() -> {
            try {
                stop();
            } catch (CameraControlException ignored) {
                // The next explicit command or session close will report its own failure.
            }
        }, command.duration().toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void stop() throws CameraControlException {
        ensureOpen();
        cancelScheduledStop();
        await(device.ptz().stop());
    }

    @Override
    public synchronized MotionEventSubscription subscribe(Consumer<MotionEvent> listener)
            throws CameraControlException {
        Objects.requireNonNull(listener, "listener must not be null");
        ensureOpen();
        var session = await(device.event().subscribe(event -> forwardMotionEvent(listener, event)));
        return new OnvifMotionEventSubscription(session, timeout);
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        cancelScheduledStop();
        closed = true;
        scheduler.shutdownNow();
    }

    private static void forwardMotionEvent(Consumer<MotionEvent> listener, Event event) {
        if (event == null || !event.isMotionRelated()) {
            return;
        }
        var occurredAt = event.getTimestamp() > 0
                ? Instant.ofEpochMilli(event.getTimestamp())
                : Instant.now();
        var type = event.getTopic() == null || event.getTopic().isBlank()
                ? "motion"
                : event.getTopic();
        var source = event.getSource() == null || event.getSource().isBlank()
                ? "onvif"
                : event.getSource();
        listener.accept(new MotionEvent(occurredAt, type, source));
    }

    private <T> T await(CompletableFuture<T> future) throws CameraControlException {
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CameraControlException("ONVIF operation was interrupted", exception);
        } catch (ExecutionException | TimeoutException | RuntimeException exception) {
            throw new CameraControlException("ONVIF operation failed", exception);
        }
    }

    private void ensureOpen() throws CameraControlException {
        if (closed) {
            throw new CameraControlException("ONVIF adapter is already closed");
        }
    }

    private void cancelScheduledStop() {
        if (scheduledStop != null) {
            scheduledStop.cancel(false);
            scheduledStop = null;
        }
    }

    private static void validateTimeout(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    private static final class OnvifMotionEventSubscription implements MotionEventSubscription {
        private final EventSession session;
        private final Duration timeout;
        private boolean closed;

        private OnvifMotionEventSubscription(EventSession session, Duration timeout) {
            this.session = session;
            this.timeout = timeout;
        }

        @Override
        public synchronized void close() throws CameraControlException {
            if (closed) {
                return;
            }
            try {
                session.unsubscribe().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                closed = true;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new CameraControlException("ONVIF event unsubscribe was interrupted", exception);
            } catch (ExecutionException | TimeoutException | RuntimeException exception) {
                throw new CameraControlException("Could not unsubscribe from ONVIF events", exception);
            }
        }
    }
}
