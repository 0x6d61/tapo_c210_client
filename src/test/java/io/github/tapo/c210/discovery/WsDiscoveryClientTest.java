package io.github.tapo.c210.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class WsDiscoveryClientTest {
    @Test
    void sendsAProbeAndCollectsUniqueCameraResponsesUntilTheTransportTimesOut() throws Exception {
        var transport = new FakeTransport(List.of(probeMatch("camera-1"), probeMatch("camera-1")));
        var client = new WsDiscoveryClient(() -> transport, new ProbeMatchParser());

        var devices = client.discover(Duration.ofSeconds(1), () -> false);

        assertEquals(1, devices.size());
        assertEquals("camera-1", devices.getFirst().deviceId());
        assertEquals("239.255.255.250", transport.target.getHostString());
        assertEquals(3702, transport.target.getPort());
        assertTrue(transport.sentMessage.contains("/Probe"));
    }

    @Test
    void stopsWithoutSendingWhenTheSearchIsCancelled() throws Exception {
        var transport = new FakeTransport(List.of());
        var client = new WsDiscoveryClient(() -> transport, new ProbeMatchParser());

        var devices = client.discover(Duration.ofSeconds(1), () -> true);

        assertTrue(devices.isEmpty());
        assertTrue(transport.sentMessage == null);
    }

    private static String probeMatch(String deviceId) {
        return """
                <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:a="http://www.w3.org/2005/08/addressing"
                    xmlns:d="http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01">
                  <s:Body><d:ProbeMatches><d:ProbeMatch>
                    <a:EndpointReference><a:Address>%s</a:Address></a:EndpointReference>
                    <d:XAddrs>http://192.168.1.20:2020/onvif/device_service</d:XAddrs>
                  </d:ProbeMatch></d:ProbeMatches></s:Body>
                </s:Envelope>
                """.formatted(deviceId);
    }

    private static final class FakeTransport implements WsDiscoveryTransport {
        private final Queue<String> responses;
        private InetSocketAddress target;
        private String sentMessage;

        private FakeTransport(List<String> responses) {
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public void send(String message, InetSocketAddress target) {
            this.sentMessage = message;
            this.target = target;
        }

        @Override
        public Optional<String> receive(Duration timeout) {
            return Optional.ofNullable(responses.poll());
        }

        @Override
        public void close() throws IOException {
        }
    }
}
