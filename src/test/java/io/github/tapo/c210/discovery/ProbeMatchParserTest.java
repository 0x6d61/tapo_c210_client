package io.github.tapo.c210.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProbeMatchParserTest {
    private final ProbeMatchParser parser = new ProbeMatchParser();

    @Test
    void extractsDeviceIdentityServiceAndModelFromProbeMatch() throws Exception {
        var devices = parser.parse("""
                <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:a="http://www.w3.org/2005/08/addressing"
                    xmlns:d="http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01">
                  <s:Body>
                    <d:ProbeMatches>
                      <d:ProbeMatch>
                        <a:EndpointReference><a:Address>urn:uuid:camera-1</a:Address></a:EndpointReference>
                        <d:Types>dn:NetworkVideoTransmitter</d:Types>
                        <d:Scopes>onvif://www.onvif.org/name/Tapo_C210 onvif://www.onvif.org/hardware/C210</d:Scopes>
                        <d:XAddrs>http://192.168.1.20:2020/onvif/device_service</d:XAddrs>
                      </d:ProbeMatch>
                    </d:ProbeMatches>
                  </s:Body>
                </s:Envelope>
                """);

        assertEquals(1, devices.size());
        assertEquals("urn:uuid:camera-1", devices.getFirst().deviceId());
        assertEquals("192.168.1.20", devices.getFirst().host());
        assertEquals(2020, devices.getFirst().onvifPort());
        assertEquals("Tapo C210", devices.getFirst().model());
        assertEquals("C210", devices.getFirst().hardwareVersion());
    }

    @Test
    void removesDuplicateMatchesForTheSameDevice() throws Exception {
        var response = """
                <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:a="http://www.w3.org/2005/08/addressing"
                    xmlns:d="http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01">
                  <s:Body><d:ProbeMatches>
                    <d:ProbeMatch>
                      <a:EndpointReference><a:Address>urn:uuid:camera-1</a:Address></a:EndpointReference>
                      <d:XAddrs>http://192.168.1.20:2020/onvif/device_service</d:XAddrs>
                    </d:ProbeMatch>
                    <d:ProbeMatch>
                      <a:EndpointReference><a:Address>urn:uuid:camera-1</a:Address></a:EndpointReference>
                      <d:XAddrs>http://192.168.1.20:2020/onvif/device_service</d:XAddrs>
                    </d:ProbeMatch>
                  </d:ProbeMatches></s:Body>
                </s:Envelope>
                """;

        assertEquals(1, parser.parse(response).size());
    }

    @Test
    void acceptsTheLegacyNamespacesUsedByTapoC210() throws Exception {
        var devices = parser.parse("""
                <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                    xmlns:wsdd="http://schemas.xmlsoap.org/ws/2005/04/discovery">
                  <SOAP-ENV:Body><wsdd:ProbeMatches><wsdd:ProbeMatch>
                    <wsa:EndpointReference><wsa:Address>uuid:c210</wsa:Address></wsa:EndpointReference>
                    <wsdd:Scopes>onvif://www.onvif.org/name/C210 onvif://www.onvif.org/hardware/C210</wsdd:Scopes>
                    <wsdd:XAddrs>http://192.168.11.15:2020/onvif/device_service</wsdd:XAddrs>
                  </wsdd:ProbeMatch></wsdd:ProbeMatches></SOAP-ENV:Body>
                </SOAP-ENV:Envelope>
                """);

        assertEquals(1, devices.size());
        assertEquals("uuid:c210", devices.getFirst().deviceId());
        assertEquals("192.168.11.15", devices.getFirst().host());
        assertEquals("C210", devices.getFirst().model());
    }

    @Test
    void rejectsXmlWithDoctypeDeclarations() {
        var xml = """
                <?xml version="1.0"?>
                <!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///secret"> ]>
                <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"><s:Body/></s:Envelope>
                """;

        assertThrows(DiscoveryParseException.class, () -> parser.parse(xml));
    }

    @Test
    void ignoresMatchesWithoutAnUsableOnvifServiceAddress() throws Exception {
        var xml = """
                <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:a="http://www.w3.org/2005/08/addressing"
                    xmlns:d="http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01">
                  <s:Body><d:ProbeMatches>
                    <d:ProbeMatch>
                      <a:EndpointReference><a:Address>urn:uuid:camera-1</a:Address></a:EndpointReference>
                      <d:XAddrs>not-a-uri</d:XAddrs>
                    </d:ProbeMatch>
                  </d:ProbeMatches></s:Body>
                </s:Envelope>
                """;

        assertEquals(List.of(), parser.parse(xml));
    }
}
