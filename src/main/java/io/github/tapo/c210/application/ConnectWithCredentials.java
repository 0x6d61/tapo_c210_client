package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.CameraProfileRepository;
import io.github.tapo.c210.application.port.RtspConnector;
import io.github.tapo.c210.application.port.SecretStore;
import io.github.tapo.c210.domain.CameraDevice;
import io.github.tapo.c210.domain.CameraProfile;
import io.github.tapo.c210.domain.CameraProfileNameGenerator;
import io.github.tapo.c210.domain.RtspEndpoint;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Opens an RTSP connection from validated form values and optionally remembers it. */
public final class ConnectWithCredentials {
    private final CameraProfileRepository profileRepository;
    private final SecretStore secretStore;
    private final RtspConnector connector;
    private final CameraProfileNameGenerator nameGenerator;
    private final Clock clock;

    public ConnectWithCredentials(
            CameraProfileRepository profileRepository,
            SecretStore secretStore,
            RtspConnector connector) {
        this(profileRepository, secretStore, connector,
                new CameraProfileNameGenerator(), Clock.systemUTC());
    }

    public ConnectWithCredentials(
            CameraProfileRepository profileRepository,
            SecretStore secretStore,
            RtspConnector connector,
            CameraProfileNameGenerator nameGenerator,
            Clock clock) {
        this.profileRepository = Objects.requireNonNull(
                profileRepository, "profileRepository must not be null");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore must not be null");
        this.connector = Objects.requireNonNull(connector, "connector must not be null");
        this.nameGenerator = Objects.requireNonNull(nameGenerator, "nameGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Opens the stream first. When {@code form.remember()} is true, the successful connection is
     * saved using metadata from discovery when available, or a deterministic manual ID otherwise.
     */
    public ConnectionResult execute(ValidatedConnectionForm form, Optional<CameraDevice> discoveredDevice)
            throws SQLException, CameraConnectionException {
        Objects.requireNonNull(form, "form must not be null");
        Objects.requireNonNull(discoveredDevice, "discoveredDevice must not be null");

        var request = new RtspConnectionRequest(
                new RtspEndpoint(form.host(), form.rtspPort(), form.streamQuality()),
                new CameraCredentials(form.username(), form.password()));
        var session = connector.connect(request);
        if (!form.remember()) {
            return new ConnectionResult(session, Optional.empty());
        }

        CameraProfile profile = null;
        try {
            profile = createProfile(form, discoveredDevice);
            profileRepository.save(profile);
            secretStore.save(profile.id(), form.password());
            return new ConnectionResult(session, Optional.of(profile));
        } catch (SQLException exception) {
            rollbackProfileAfterPersistenceFailure(profile, exception);
            return new ConnectionResult(
                    session,
                    Optional.empty(),
                    Optional.of("接続は成功しましたが、接続情報を保存できませんでした。"));
        } catch (RuntimeException exception) {
            closeAfterPersistenceFailure(session, exception);
            throw exception;
        }
    }

    private CameraProfile createProfile(
            ValidatedConnectionForm form, Optional<CameraDevice> discoveredDevice) throws SQLException {
        var profiles = profileRepository.list();
        var id = discoveredDevice.map(CameraDevice::deviceId)
                .orElseGet(() -> manualProfileId(form));
        var existing = profiles.stream()
                .filter(profile -> profile.id().equals(id))
                .findFirst();
        var displayName = existing.map(CameraProfile::displayName)
                .orElseGet(() -> nameGenerator.generate(
                        discoveredDevice.map(CameraDevice::model).orElse(null),
                        form.host(),
                        profiles.stream().map(CameraProfile::displayName).toList()));
        var deviceId = discoveredDevice.map(CameraDevice::deviceId).orElse(null);
        var onvifPort = discoveredDevice.map(CameraDevice::onvifPort).orElse(form.onvifPort());
        var now = Instant.now(clock);
        return new CameraProfile(
                id,
                displayName,
                deviceId,
                form.host(),
                onvifPort,
                form.rtspPort(),
                form.username(),
                form.streamQuality(),
                now);
    }

    private static String manualProfileId(ValidatedConnectionForm form) {
        return "manual:%s:%d".formatted(form.host(), form.onvifPort());
    }

    private static void closeAfterPersistenceFailure(ConnectedCamera session, Exception failure) {
        try {
            session.close();
        } catch (Exception closeException) {
            failure.addSuppressed(closeException);
        }
    }

    private void rollbackProfileAfterPersistenceFailure(CameraProfile profile, SQLException failure) {
        if (profile == null) {
            return;
        }
        try {
            profileRepository.delete(profile.id());
        } catch (SQLException rollbackException) {
            failure.addSuppressed(rollbackException);
        }
    }
}
