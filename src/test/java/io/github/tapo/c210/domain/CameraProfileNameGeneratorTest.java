package io.github.tapo.c210.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CameraProfileNameGeneratorTest {
    private final CameraProfileNameGenerator generator = new CameraProfileNameGenerator();

    @Test
    void generatesNameFromModelAndHost() {
        assertEquals(
                "Tapo C210 (192.168.1.20)",
                generator.generate("Tapo C210", "192.168.1.20", List.of()));
    }

    @Test
    void fallsBackToCameraWhenModelIsUnavailable() {
        assertEquals(
                "Camera (192.168.1.20)",
                generator.generate(" ", "192.168.1.20", List.of()));
    }

    @Test
    void addsAnIncrementingSuffixWhenNameAlreadyExists() {
        var existingNames = List.of(
                "Tapo C210 (192.168.1.20)",
                "Tapo C210 (192.168.1.20) #2");

        assertEquals(
                "Tapo C210 (192.168.1.20) #3",
                generator.generate("Tapo C210", "192.168.1.20", existingNames));
    }
}
