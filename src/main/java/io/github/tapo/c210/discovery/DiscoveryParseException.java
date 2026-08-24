package io.github.tapo.c210.discovery;

/** Indicates that a WS-Discovery response could not be safely parsed. */
public class DiscoveryParseException extends Exception {
    public DiscoveryParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
