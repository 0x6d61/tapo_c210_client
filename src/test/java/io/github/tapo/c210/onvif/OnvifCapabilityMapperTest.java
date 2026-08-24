package io.github.tapo.c210.onvif;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hyeonmo.models.OnvifCapabilities;
import org.junit.jupiter.api.Test;

class OnvifCapabilityMapperTest {
    @Test
    void mapsAdvertisedPtzAndEventEndpoints() {
        var capabilities = new OnvifCapabilities();
        capabilities.setPtzXaddr("http://192.168.1.20/onvif/ptz_service");
        capabilities.setEventsXaddr("http://192.168.1.20/onvif/event_service");

        var result = OnvifCapabilityMapper.toDomain(capabilities);

        assertTrue(result.ptz());
        assertTrue(result.motionEvents());
        assertFalse(result.localRecording());
        assertFalse(result.talkback());
        assertFalse(result.cameraStorageRecording());
    }

    @Test
    void treatsMissingOptionalEndpointsAsUnsupported() {
        var result = OnvifCapabilityMapper.toDomain(new OnvifCapabilities());

        assertFalse(result.ptz());
        assertFalse(result.motionEvents());
    }
}
