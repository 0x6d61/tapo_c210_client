package io.github.tapo.c210.discovery;

import io.github.tapo.c210.application.CameraDiscoveryException;
import io.github.tapo.c210.application.port.CameraDiscovery;
import io.github.tapo.c210.domain.CameraDevice;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Discovers ONVIF devices through the WS-Discovery multicast endpoint. */
public final class WsDiscoveryClient implements CameraDiscovery {
    public static final String MULTICAST_ADDRESS = "239.255.255.250";
    public static final int MULTICAST_PORT = 3702;

    private final WsDiscoveryTransportFactory transportFactory;
    private final ProbeMatchParser parser;

    public WsDiscoveryClient() {
        this(UdpWsDiscoveryTransport::new, new ProbeMatchParser());
    }

    public WsDiscoveryClient(
            WsDiscoveryTransportFactory transportFactory,
            ProbeMatchParser parser) {
        this.transportFactory = Objects.requireNonNull(
                transportFactory, "transportFactory must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
    }

    @Override
    public List<CameraDevice> discover(Duration timeout, BooleanSupplier cancelled)
            throws CameraDiscoveryException {
        Objects.requireNonNull(timeout, "timeout must not be null");
        Objects.requireNonNull(cancelled, "cancelled must not be null");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (cancelled.getAsBoolean()) {
            return List.of();
        }

        var devices = new LinkedHashMap<String, CameraDevice>();
        var deadline = System.nanoTime() + timeout.toNanos();
        try (var transport = transportFactory.open()) {
            transport.send(
                    WsDiscoveryProbeMessage.create(),
                    new InetSocketAddress(MULTICAST_ADDRESS, MULTICAST_PORT));
            while (!cancelled.getAsBoolean()) {
                var remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0) {
                    break;
                }
                var response = transport.receive(Duration.ofNanos(remainingNanos));
                if (response.isEmpty()) {
                    break;
                }
                try {
                    for (var device : parser.parse(response.get())) {
                        devices.putIfAbsent(device.deviceId(), device);
                    }
                } catch (DiscoveryParseException ignored) {
                    // Ignore one malformed datagram and keep collecting other cameras.
                }
            }
            return List.copyOf(devices.values());
        } catch (IOException exception) {
            throw new CameraDiscoveryException("Could not complete WS-Discovery", exception);
        }
    }
}
