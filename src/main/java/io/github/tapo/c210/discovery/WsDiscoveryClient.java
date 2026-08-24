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
    private static final Duration RESPONSE_QUIET_PERIOD = Duration.ofMillis(750);

    private final WsDiscoveryTransportFactory transportFactory;
    private final ProbeMatchParser parser;
    private final WsDiscoveryTargetProvider targetProvider;

    public WsDiscoveryClient() {
        this(UdpWsDiscoveryTransport::new, new ProbeMatchParser(), LocalSubnetTargetProvider::targets);
    }

    public WsDiscoveryClient(
            WsDiscoveryTransportFactory transportFactory,
            ProbeMatchParser parser) {
        this(transportFactory, parser, List::of);
    }

    WsDiscoveryClient(
            WsDiscoveryTransportFactory transportFactory,
            ProbeMatchParser parser,
            WsDiscoveryTargetProvider targetProvider) {
        this.transportFactory = Objects.requireNonNull(
                transportFactory, "transportFactory must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.targetProvider = Objects.requireNonNull(
                targetProvider, "targetProvider must not be null");
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
            var probe = WsDiscoveryProbeMessage.create();
            transport.send(probe, new InetSocketAddress(MULTICAST_ADDRESS, MULTICAST_PORT));
            if (!cancelled.getAsBoolean()) {
                var sentUnicastProbes = 0;
                for (var target : targetProvider.targets()) {
                    if (cancelled.getAsBoolean() || System.nanoTime() >= deadline) {
                        break;
                    }
                    transport.send(probe, target);
                    sentUnicastProbes++;
                    if (sentUnicastProbes % 16 == 0) {
                        collectResponse(transport, devices, Duration.ofMillis(1));
                        if (!devices.isEmpty()) {
                            break;
                        }
                    }
                }
            }
            while (!cancelled.getAsBoolean()) {
                var remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0) {
                    break;
                }
                var receiveNanos = devices.isEmpty()
                        ? remainingNanos
                        : Math.min(remainingNanos, RESPONSE_QUIET_PERIOD.toNanos());
                var response = transport.receive(Duration.ofNanos(receiveNanos));
                if (response.isEmpty()) {
                    break;
                }
                collectResponse(response.get(), devices);
            }
            return List.copyOf(devices.values());
        } catch (IOException exception) {
            throw new CameraDiscoveryException("Could not complete WS-Discovery", exception);
        }
    }

    private void collectResponse(
            WsDiscoveryTransport transport,
            LinkedHashMap<String, CameraDevice> devices,
            Duration timeout) throws IOException {
        var response = transport.receive(timeout);
        if (response.isPresent()) {
            collectResponse(response.get(), devices);
        }
    }

    private void collectResponse(String response, LinkedHashMap<String, CameraDevice> devices) {
        try {
            for (var device : parser.parse(response)) {
                devices.putIfAbsent(device.deviceId(), device);
            }
        } catch (DiscoveryParseException ignored) {
            // Ignore one malformed datagram and keep collecting other cameras.
        }
    }
}
