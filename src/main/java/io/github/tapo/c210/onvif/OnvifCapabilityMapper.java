package io.github.tapo.c210.onvif;

import io.github.hyeonmo.models.OnvifCapabilities;
import io.github.tapo.c210.domain.CameraCapabilities;
import java.util.Objects;

/** Maps ONVIF service endpoints to the application capability model. */
final class OnvifCapabilityMapper {
    private OnvifCapabilityMapper() {
    }

    static CameraCapabilities toDomain(OnvifCapabilities capabilities) {
        Objects.requireNonNull(capabilities, "capabilities must not be null");
        return new CameraCapabilities(
                hasValue(capabilities.getPtzXaddr()),
                false,
                false,
                hasValue(capabilities.getEventsXaddr()),
                false);
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
