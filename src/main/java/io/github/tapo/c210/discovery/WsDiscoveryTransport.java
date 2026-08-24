package io.github.tapo.c210.discovery;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

/** Datagram boundary used by the WS-Discovery client and its tests. */
public interface WsDiscoveryTransport extends AutoCloseable {
    void send(String message, InetSocketAddress target) throws IOException;

    Optional<String> receive(Duration timeout) throws IOException;

    @Override
    void close() throws IOException;
}
