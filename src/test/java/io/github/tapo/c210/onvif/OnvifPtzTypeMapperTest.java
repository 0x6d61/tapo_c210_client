package io.github.tapo.c210.onvif;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.hyeonmo.models.ptz.PtzType;
import io.github.tapo.c210.domain.PtzDirection;
import org.junit.jupiter.api.Test;

class OnvifPtzTypeMapperTest {
    @Test
    void mapsPanTiltAndZoomDirections() {
        assertEquals(PtzType.LEFT, OnvifPtzTypeMapper.toOnvif(PtzDirection.PAN_LEFT));
        assertEquals(PtzType.RIGHT, OnvifPtzTypeMapper.toOnvif(PtzDirection.PAN_RIGHT));
        assertEquals(PtzType.UP, OnvifPtzTypeMapper.toOnvif(PtzDirection.TILT_UP));
        assertEquals(PtzType.DOWN, OnvifPtzTypeMapper.toOnvif(PtzDirection.TILT_DOWN));
        assertEquals(PtzType.ZOOM_IN, OnvifPtzTypeMapper.toOnvif(PtzDirection.ZOOM_IN));
        assertEquals(PtzType.ZOOM_OUT, OnvifPtzTypeMapper.toOnvif(PtzDirection.ZOOM_OUT));
    }
}
