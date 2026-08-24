package io.github.tapo.c210.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tapo.c210.application.port.CameraProfileRepository;
import io.github.tapo.c210.application.port.RtspConnector;
import io.github.tapo.c210.application.port.SecretStore;
import io.github.tapo.c210.domain.CameraDevice;
import io.github.tapo.c210.domain.CameraProfile;
import io.github.tapo.c210.domain.StreamQuality;
import java.net.URI;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConnectWithCredentialsTest {
    private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

    @Test
    void connectsWithoutPersistingWhenRememberIsDisabled() throws Exception {
        var repository = new InMemoryProfileRepository();
        var secrets = new InMemorySecretStore();
        var connector = new CapturingConnector();
        var session = new TestSession();
        connector.session = session;

        var result = new ConnectWithCredentials(repository, secrets, connector)
                .execute(form(false), Optional.empty());

        assertEquals(session, result.session());
        assertTrue(result.savedProfile().isEmpty());
        assertTrue(repository.saved.isEmpty());
        assertTrue(secrets.passwords.isEmpty());
        assertEquals("192.168.1.20", connector.request.endpoint().toUri().getHost());
        assertEquals("/stream1", connector.request.endpoint().toUri().getPath());
    }

    @Test
    void generatesAndPersistsAProfileForARememberedDiscoveredCamera() throws Exception {
        var existing = new CameraProfile(
                "other-camera",
                "Tapo C210 (192.168.1.30)",
                "other-device",
                "192.168.1.30",
                2020,
                554,
                "other-user",
                StreamQuality.HIGH,
                NOW);
        var repository = new InMemoryProfileRepository(existing);
        var secrets = new InMemorySecretStore();
        var connector = new CapturingConnector();
        connector.session = new TestSession();

        var result = new ConnectWithCredentials(
                repository,
                secrets,
                connector,
                new io.github.tapo.c210.domain.CameraProfileNameGenerator(),
                Clock.fixed(NOW, ZoneOffset.UTC))
                .execute(form(true), Optional.of(discoveredDevice()));

        assertTrue(result.savedProfile().isPresent());
        var profile = result.savedProfile().orElseThrow();
        assertEquals("onvif-device-1", profile.id());
        assertEquals("Tapo C210 (192.168.1.20)", profile.displayName());
        assertEquals("192.168.1.20", profile.host());
        assertEquals(2020, profile.onvifPort());
        assertEquals(StreamQuality.HIGH, profile.streamQuality());
        assertEquals("camera-password", secrets.passwords.get(profile.id()));
        assertEquals(profile, repository.saved.get(0));
    }

    @Test
    void reusesTheExistingDisplayNameWhenTheSameCameraIsRememberedAgain() throws Exception {
        var existing = new CameraProfile(
                "onvif-device-1",
                "Living room camera",
                "onvif-device-1",
                "192.168.1.20",
                2020,
                554,
                "old-user",
                StreamQuality.LOW,
                NOW.minusSeconds(60));
        var repository = new InMemoryProfileRepository(existing);
        var secrets = new InMemorySecretStore();
        var connector = new CapturingConnector();
        connector.session = new TestSession();

        var result = new ConnectWithCredentials(
                repository, secrets, connector,
                new io.github.tapo.c210.domain.CameraProfileNameGenerator(),
                Clock.fixed(NOW, ZoneOffset.UTC))
                .execute(form(true), Optional.of(discoveredDevice()));

        assertEquals("Living room camera", result.savedProfile().orElseThrow().displayName());
        assertEquals(StreamQuality.HIGH, result.savedProfile().orElseThrow().streamQuality());
    }

    @Test
    void keepsTheSessionOpenWhenPersistenceFails() throws Exception {
        var repository = new FailingProfileRepository();
        var connector = new CapturingConnector();
        var session = new TestSession();
        connector.session = session;

        var result = new ConnectWithCredentials(
                repository,
                new InMemorySecretStore(),
                connector)
                .execute(form(true), Optional.empty());

        assertEquals(session, result.session());
        assertTrue(result.savedProfile().isEmpty());
        assertTrue(result.persistenceWarning().isPresent());
        assertFalse(session.closed);
    }

    private static ValidatedConnectionForm form(boolean remember) {
        return new ValidatedConnectionForm(
                "192.168.1.20", 2020, 554, "camera-user", "camera-password",
                StreamQuality.HIGH, remember);
    }

    private static CameraDevice discoveredDevice() {
        return new CameraDevice(
                "onvif-device-1",
                "192.168.1.20",
                2020,
                554,
                URI.create("http://192.168.1.20/onvif/device_service"),
                "TP-Link",
                "Tapo C210",
                null);
    }

    private static final class InMemoryProfileRepository implements CameraProfileRepository {
        private final List<CameraProfile> profiles = new ArrayList<>();
        private final List<CameraProfile> saved = new ArrayList<>();

        private InMemoryProfileRepository(CameraProfile... profiles) {
            this.profiles.addAll(List.of(profiles));
        }

        @Override
        public void save(CameraProfile profile) {
            saved.add(profile);
            profiles.removeIf(existing -> existing.id().equals(profile.id()));
            profiles.add(profile);
        }

        @Override
        public List<CameraProfile> list() {
            return List.copyOf(profiles);
        }

        @Override
        public void delete(String profileId) {
            profiles.removeIf(profile -> profile.id().equals(profileId));
        }
    }

    private static final class FailingProfileRepository implements CameraProfileRepository {
        @Override
        public void save(CameraProfile profile) throws SQLException {
            throw new SQLException("profile write failed");
        }

        @Override
        public List<CameraProfile> list() {
            return List.of();
        }

        @Override
        public void delete(String profileId) {
        }
    }

    private static final class InMemorySecretStore implements SecretStore {
        private final java.util.Map<String, String> passwords = new java.util.HashMap<>();

        @Override
        public void save(String profileId, String password) {
            passwords.put(profileId, password);
        }

        @Override
        public Optional<String> load(String profileId) {
            return Optional.ofNullable(passwords.get(profileId));
        }

        @Override
        public void delete(String profileId) {
            passwords.remove(profileId);
        }
    }

    private static final class CapturingConnector implements RtspConnector {
        private RtspConnectionRequest request;
        private ConnectedCamera session;

        @Override
        public ConnectedCamera connect(RtspConnectionRequest request) {
            this.request = request;
            return session;
        }
    }

    private static final class TestSession implements ConnectedCamera {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }
}
