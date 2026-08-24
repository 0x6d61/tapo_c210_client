package io.github.tapo.c210.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Generates stable, user-input-free names for saved camera profiles. */
public final class CameraProfileNameGenerator {
    /**
     * Generates a model-and-host name and adds a numeric suffix when needed.
     *
     * @param model the detected model name, or {@code null} when unavailable
     * @param host the camera host
     * @param existingNames names already used by saved profiles
     * @return a unique display name
     */
    public String generate(String model, String host, Collection<String> existingNames) {
        Objects.requireNonNull(host, "host must not be null");
        Objects.requireNonNull(existingNames, "existingNames must not be null");
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }

        var base = model == null || model.isBlank() ? "Camera" : model.trim();
        var candidateBase = "%s (%s)".formatted(base, host.trim());
        var names = new HashSet<>(existingNames);
        var candidate = candidateBase;
        var suffix = 2;
        while (names.contains(candidate)) {
            candidate = "%s #%d".formatted(candidateBase, suffix++);
        }
        return candidate;
    }
}
