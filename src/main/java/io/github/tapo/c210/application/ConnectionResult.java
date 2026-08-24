package io.github.tapo.c210.application;

import io.github.tapo.c210.domain.CameraProfile;
import java.util.Objects;
import java.util.Optional;

/** Result of opening a camera stream, optionally including the saved profile. */
public record ConnectionResult(
        ConnectedCamera session,
        Optional<CameraProfile> savedProfile,
        Optional<String> persistenceWarning) {
    public ConnectionResult(ConnectedCamera session, Optional<CameraProfile> savedProfile) {
        this(session, savedProfile, Optional.empty());
    }

    public ConnectionResult {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(savedProfile, "savedProfile must not be null");
        Objects.requireNonNull(persistenceWarning, "persistenceWarning must not be null");
    }
}
