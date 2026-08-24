package io.github.tapo.c210.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tapo.c210.application.port.CameraProfileRepository;
import io.github.tapo.c210.application.port.RtspConnector;
import io.github.tapo.c210.application.port.SecretStore;
import io.github.tapo.c210.domain.CameraProfile;
import io.github.tapo.c210.domain.StreamQuality;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConnectWithProfileTest {
    @Test
    void loadsTheSelectedPasswordAndConnectsToTheSavedStream() throws Exception {
        var profile = profile();
        var repository = new InMemoryProfileRepository(profile);
        var secrets = new InMemorySecretStore("camera-password");
        var connector = new CapturingConnector();
        var session = new TestSession();
        connector.session = session;

        var result = new ConnectWithProfile(repository, secrets, connector).execute("profile-1");

        assertEquals(session, result);
        assertEquals("192.168.1.20", connector.request.endpoint().host());
        assertEquals(554, connector.request.endpoint().port());
        assertEquals(StreamQuality.HIGH, connector.request.endpoint().quality());
        assertEquals("camera-user", connector.request.credentials().username());
        assertEquals("camera-password", connector.request.credentials().password());
        assertFalse(connector.request.toString().contains("camera-password"));
    }

    @Test
    void refusesToConnectWhenTheSelectedProfileHasNoPassword() {
        var connector = new CapturingConnector();
        var useCase = new ConnectWithProfile(
                new InMemoryProfileRepository(profile()),
                new InMemorySecretStore(null),
                connector);

        assertThrows(
                MissingCameraPasswordException.class,
                () -> useCase.execute("profile-1"));
        assertTrue(connector.request == null);
    }

    @Test
    void refusesToConnectWhenTheProfileDoesNotExist() {
        var connector = new CapturingConnector();
        var useCase = new ConnectWithProfile(
                new InMemoryProfileRepository(profile()),
                new InMemorySecretStore("camera-password"),
                connector);

        assertThrows(ProfileNotFoundException.class, () -> useCase.execute("missing"));
        assertTrue(connector.request == null);
    }

    private static CameraProfile profile() {
        return new CameraProfile(
                "profile-1",
                "Tapo C210 (192.168.1.20)",
                "onvif-device-1",
                "192.168.1.20",
                2020,
                554,
                "camera-user",
                StreamQuality.HIGH,
                Instant.parse("2026-08-24T12:00:00Z"));
    }

    private static final class InMemoryProfileRepository implements CameraProfileRepository {
        private final CameraProfile profile;

        private InMemoryProfileRepository(CameraProfile profile) {
            this.profile = profile;
        }

        @Override
        public void save(CameraProfile profile) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<CameraProfile> list() throws SQLException {
            return List.of(profile);
        }

        @Override
        public void delete(String profileId) throws SQLException {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemorySecretStore implements SecretStore {
        private final String password;

        private InMemorySecretStore(String password) {
            this.password = password;
        }

        @Override
        public void save(String profileId, String password) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<String> load(String profileId) throws SQLException {
            return Optional.ofNullable(password);
        }

        @Override
        public void delete(String profileId) throws SQLException {
            throw new UnsupportedOperationException();
        }
    }

    private static final class CapturingConnector implements RtspConnector {
        private RtspConnectionRequest request;
        private ConnectedCamera session;

        @Override
        public ConnectedCamera connect(RtspConnectionRequest request)
                throws CameraConnectionException {
            this.request = request;
            return session;
        }
    }

    private static final class TestSession implements ConnectedCamera {
        @Override
        public void close() {
        }
    }
}
