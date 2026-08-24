package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.CameraProfileRepository;
import io.github.tapo.c210.application.port.SecretStore;
import io.github.tapo.c210.domain.CameraProfile;
import java.sql.SQLException;
import java.util.Objects;

/** Updates all connection fields of a saved camera profile and its local password. */
public final class UpdateSavedCameraProfile {
    private final CameraProfileRepository profileRepository;
    private final SecretStore secretStore;

    public UpdateSavedCameraProfile(
            CameraProfileRepository profileRepository,
            SecretStore secretStore) {
        this.profileRepository = Objects.requireNonNull(
                profileRepository, "profileRepository must not be null");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore must not be null");
    }

    public void execute(CameraProfile profile, ValidatedConnectionForm form)
            throws SQLException {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(form, "form must not be null");

        profileRepository.save(new CameraProfile(
                profile.id(),
                profile.displayName(),
                profile.deviceId(),
                form.host(),
                form.onvifPort(),
                form.rtspPort(),
                form.username(),
                form.streamQuality(),
                profile.lastUsedAt()));
        secretStore.save(profile.id(), form.password());
    }
}
