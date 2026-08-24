package io.github.tapo.c210.application;

import io.github.tapo.c210.domain.StreamQuality;

/** Raw values collected by the connection form before validation. */
public record ConnectionFormInput(
        String host,
        String onvifPort,
        String rtspPort,
        String username,
        String password,
        StreamQuality streamQuality,
        boolean remember) {
}
