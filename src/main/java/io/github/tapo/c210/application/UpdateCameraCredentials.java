package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.CameraProfileRepository;
import io.github.tapo.c210.application.port.SecretStore;
import io.github.tapo.c210.domain.CameraProfile;
import java.sql.SQLException;
import java.util.Objects;

/** Updates a saved profile's username and, when supplied, its locally stored password. */
public final class UpdateCameraCredentials {
    private final CameraProfileRepository profileRepository;
    private final SecretStore secretStore;

    public UpdateCameraCredentials(
            CameraProfileRepository profileRepository,
            SecretStore secretStore) {
        this.profileRepository = Objects.requireNonNull(
                profileRepository, "profileRepository must not be null");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore must not be null");
    }

    public void execute(CameraProfile profile, String username, String password)
            throws SQLException {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(password, "password must not be null");
        var normalizedUsername = username.trim();
        if (normalizedUsername.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }

        profileRepository.save(new CameraProfile(
                profile.id(),
                profile.displayName(),
                profile.deviceId(),
                profile.host(),
                profile.onvifPort(),
                profile.rtspPort(),
                normalizedUsername,
                profile.streamQuality(),
                profile.lastUsedAt()));
        if (!password.isBlank()) {
            secretStore.save(profile.id(), password);
        }
    }
}
