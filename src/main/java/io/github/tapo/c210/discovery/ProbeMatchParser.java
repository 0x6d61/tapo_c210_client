package io.github.tapo.c210.discovery;

import io.github.tapo.c210.domain.CameraDevice;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/** Parses and de-duplicates ONVIF WS-Discovery ProbeMatch responses. */
public final class ProbeMatchParser {
    private static final String ADDRESSING_NS = "http://www.w3.org/2005/08/addressing";
    private static final String DISCOVERY_NS = "http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01";

    public List<CameraDevice> parse(String xml) throws DiscoveryParseException {
        try {
            var builder = secureFactory().newDocumentBuilder();
            builder.setErrorHandler(new ThrowingErrorHandler());
            var document = builder.parse(new InputSource(new StringReader(xml)));
            var devices = new LinkedHashMap<String, CameraDevice>();
            var matches = document.getElementsByTagNameNS(DISCOVERY_NS, "ProbeMatch");
            for (var index = 0; index < matches.getLength(); index++) {
                var match = (Element) matches.item(index);
                var deviceId = text(match, ADDRESSING_NS, "Address");
                var serviceUrl = firstServiceUrl(text(match, DISCOVERY_NS, "XAddrs"));
                if (deviceId == null || serviceUrl == null || devices.containsKey(deviceId)) {
                    continue;
                }
                var scopes = split(text(match, DISCOVERY_NS, "Scopes"));
                var host = serviceUrl.getHost();
                if (host == null || host.isBlank()) {
                    continue;
                }
                var onvifPort = serviceUrl.getPort() > 0
                        ? serviceUrl.getPort()
                        : ("https".equalsIgnoreCase(serviceUrl.getScheme()) ? 443 : 80);
                try {
                    devices.put(deviceId, new CameraDevice(
                            deviceId,
                            host,
                            onvifPort,
                            554,
                            serviceUrl,
                            scopeValue(scopes, "/manufacturer/"),
                            scopeValue(scopes, "/name/"),
                            scopeValue(scopes, "/hardware/")));
                } catch (IllegalArgumentException ignored) {
                    // A malformed response must not prevent valid cameras from being listed.
                }
            }
            return List.copyOf(devices.values());
        } catch (ParserConfigurationException | SAXException | IOException | RuntimeException exception) {
            throw new DiscoveryParseException("Could not parse WS-Discovery response", exception);
        }
    }

    private static DocumentBuilderFactory secureFactory() throws ParserConfigurationException {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    private static String text(Element parent, String namespace, String localName) {
        var nodes = parent.getElementsByTagNameNS(namespace, localName);
        if (nodes.getLength() == 0) {
            return null;
        }
        var value = nodes.item(0).getTextContent();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static URI firstServiceUrl(String xAddrs) {
        if (xAddrs == null) {
            return null;
        }
        for (var address : xAddrs.split("\\s+")) {
            try {
                var uri = URI.create(address);
                if (uri.getHost() != null
                        && ("http".equalsIgnoreCase(uri.getScheme())
                        || "https".equalsIgnoreCase(uri.getScheme()))) {
                    return uri;
                }
            } catch (IllegalArgumentException ignored) {
                // ProbeMatch can contain multiple addresses; try the next one.
            }
        }
        return null;
    }

    private static List<String> split(String value) {
        if (value == null) {
            return List.of();
        }
        var result = new ArrayList<String>();
        for (var scope : value.split("\\s+")) {
            if (!scope.isBlank()) {
                result.add(scope);
            }
        }
        return result;
    }

    private static String scopeValue(List<String> scopes, String marker) {
        for (var scope : scopes) {
            var index = scope.indexOf(marker);
            if (index >= 0) {
                var value = scope.substring(index + marker.length());
                return URLDecoder.decode(value, StandardCharsets.UTF_8).replace('_', ' ');
            }
        }
        return null;
    }

    private static final class ThrowingErrorHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException exception) throws SAXParseException {
            throw exception;
        }

        @Override
        public void error(SAXParseException exception) throws SAXParseException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXParseException {
            throw exception;
        }
    }
}
