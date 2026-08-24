package io.github.tapo.c210.discovery;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

/** Supplies additional WS-Discovery destinations used when multicast is unavailable. */
@FunctionalInterface
public interface WsDiscoveryTargetProvider {
    List<InetSocketAddress> targets() throws IOException;
}
