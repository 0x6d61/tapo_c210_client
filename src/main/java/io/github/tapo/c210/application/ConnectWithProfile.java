package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.CameraProfileRepository;
import io.github.tapo.c210.application.port.RtspConnector;
import io.github.tapo.c210.application.port.SecretStore;
import io.github.tapo.c210.domain.CameraProfile;
import io.github.tapo.c210.domain.RtspEndpoint;
import java.sql.SQLException;
import java.util.Objects;

/** Opens an RTSP connection using a selected saved profile. */
public final class ConnectWithProfile {
    private final CameraProfileRepository profileRepository;
    private final SecretStore secretStore;
    private final RtspConnector connector;

    public ConnectWithProfile(
            CameraProfileRepository profileRepository,
            SecretStore secretStore,
            RtspConnector connector) {
        this.profileRepository = Objects.requireNonNull(
                profileRepository, "profileRepository must not be null");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore must not be null");
        this.connector = Objects.requireNonNull(connector, "connector must not be null");
    }

    public ConnectedCamera execute(String profileId)
            throws SQLException, ProfileNotFoundException, MissingCameraPasswordException,
                    CameraConnectionException {
        Objects.requireNonNull(profileId, "profileId must not be null");
        var profile = findProfile(profileId);
        var password = secretStore.load(profile.id())
                .orElseThrow(() -> new MissingCameraPasswordException(profile.id()));
        var request = new RtspConnectionRequest(
                new RtspEndpoint(profile.host(), profile.rtspPort(), profile.streamQuality()),
                new CameraCredentials(profile.username(), password));
        return connector.connect(request);
    }

    private CameraProfile findProfile(String profileId) throws SQLException, ProfileNotFoundException {
        return profileRepository.list().stream()
                .filter(profile -> profile.id().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
    }
}
