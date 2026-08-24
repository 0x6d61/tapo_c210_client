package io.github.tapo.c210.onvif;

import io.github.hyeonmo.models.ptz.PtzType;
import io.github.tapo.c210.domain.PtzDirection;
import java.util.Objects;

/** Maps the application PTZ command directions to the ONVIF client library. */
final class OnvifPtzTypeMapper {
    private OnvifPtzTypeMapper() {
    }

    static PtzType toOnvif(PtzDirection direction) {
        Objects.requireNonNull(direction, "direction must not be null");
        return switch (direction) {
            case PAN_LEFT -> PtzType.LEFT;
            case PAN_RIGHT -> PtzType.RIGHT;
            case TILT_UP -> PtzType.UP;
            case TILT_DOWN -> PtzType.DOWN;
            case ZOOM_IN -> PtzType.ZOOM_IN;
            case ZOOM_OUT -> PtzType.ZOOM_OUT;
        };
    }
}
