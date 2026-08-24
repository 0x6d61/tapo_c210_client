package io.github.tapo.c210.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/** UDP transport for WS-Discovery responses sent back to the ephemeral client port. */
public final class UdpWsDiscoveryTransport implements WsDiscoveryTransport {
    private static final int MAX_DATAGRAM_SIZE = 65_507;
    private final DatagramSocket socket;

    public UdpWsDiscoveryTransport() throws IOException {
        socket = new DatagramSocket();
    }

    @Override
    public void send(String message, InetSocketAddress target) throws IOException {
        var payload = message.getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(payload, payload.length, target));
    }

    @Override
    public Optional<String> receive(Duration timeout) throws IOException {
        if (timeout.isZero() || timeout.isNegative()) {
            return Optional.empty();
        }
        var timeoutMillis = Math.max(1, Math.min(Integer.MAX_VALUE, timeout.toMillis()));
        socket.setSoTimeout((int) timeoutMillis);
        var packet = new DatagramPacket(new byte[MAX_DATAGRAM_SIZE], MAX_DATAGRAM_SIZE);
        try {
            socket.receive(packet);
            return Optional.of(new String(
                    packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8));
        } catch (SocketTimeoutException timeoutException) {
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        socket.close();
    }
}
