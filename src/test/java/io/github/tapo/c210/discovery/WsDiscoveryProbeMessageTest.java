package io.github.tapo.c210.discovery;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WsDiscoveryProbeMessageTest {
    @Test
    void createsAProbeWithTheRequiredSoapHeadersAndBody() {
        var message = WsDiscoveryProbeMessage.create("urn:uuid:test-message");

        assertTrue(message.contains("http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01/Probe"));
        assertTrue(message.contains("<a:MessageID>urn:uuid:test-message</a:MessageID>"));
        assertTrue(message.contains("<d:Probe"));
        assertTrue(message.contains("dn:NetworkVideoTransmitter"));
    }
}
