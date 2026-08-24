package io.github.tapo.c210.discovery;

import java.util.Objects;
import java.util.UUID;

/** Builds the SOAP Probe message sent to the WS-Discovery multicast address. */
public final class WsDiscoveryProbeMessage {
    private WsDiscoveryProbeMessage() {
    }

    public static String create() {
        return create("urn:uuid:" + UUID.randomUUID());
    }

    public static String create(String messageId) {
        Objects.requireNonNull(messageId, "messageId must not be null");
        if (messageId.isBlank()) {
            throw new IllegalArgumentException("messageId must not be blank");
        }
        var escapedMessageId = escapeXml(messageId);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:a="http://www.w3.org/2005/08/addressing"
                    xmlns:d="http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01"
                    xmlns:dn="http://www.onvif.org/ver10/network/wsdl">
                  <s:Header>
                    <a:Action>http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01/Probe</a:Action>
                    <a:MessageID>%s</a:MessageID>
                    <a:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</a:To>
                  </s:Header>
                  <s:Body>
                    <d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe>
                  </s:Body>
                </s:Envelope>
                """.formatted(escapedMessageId);
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
