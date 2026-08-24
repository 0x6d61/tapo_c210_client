package io.github.tapo.c210.onvif;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.hyeonmo.models.ptz.PtzType;
import io.github.tapo.c210.domain.PtzDirection;
import org.junit.jupiter.api.Test;

class OnvifPtzTypeMapperTest {
    @Test
    void mapsPanAndTiltDirections() {
        assertEquals(PtzType.LEFT, OnvifPtzTypeMapper.toOnvif(PtzDirection.PAN_LEFT));
        assertEquals(PtzType.RIGHT, OnvifPtzTypeMapper.toOnvif(PtzDirection.PAN_RIGHT));
        assertEquals(PtzType.UP, OnvifPtzTypeMapper.toOnvif(PtzDirection.TILT_UP));
        assertEquals(PtzType.DOWN, OnvifPtzTypeMapper.toOnvif(PtzDirection.TILT_DOWN));
    }
}
