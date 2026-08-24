package io.github.tapo.c210.discovery;

import java.io.IOException;

/** Creates a WS-Discovery transport for one discovery operation. */
@FunctionalInterface
public interface WsDiscoveryTransportFactory {
    WsDiscoveryTransport open() throws IOException;
}
