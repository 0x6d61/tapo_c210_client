package io.github.tapo.c210.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.tapo.c210.application.port.CameraProfileRepository;
import io.github.tapo.c210.application.port.SecretStore;
import io.github.tapo.c210.domain.CameraProfile;
import io.github.tapo.c210.domain.StreamQuality;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateSavedCameraProfileTest {
    @Test
    void updatesAllConnectionFieldsAndThePassword() throws Exception {
        var repository = new CapturingProfileRepository();
        var secrets = new CapturingSecretStore();
        var form = new ValidatedConnectionForm(
                "192.168.1.30",
                2021,
                855,
                "new-user",
                "new-password",
                StreamQuality.LOW,
                true);

        new UpdateSavedCameraProfile(repository, secrets).execute(profile(), form);

        assertEquals("192.168.1.30", repository.saved.host());
        assertEquals(2021, repository.saved.onvifPort());
        assertEquals(855, repository.saved.rtspPort());
        assertEquals("new-user", repository.saved.username());
        assertEquals(StreamQuality.LOW, repository.saved.streamQuality());
        assertEquals("new-password", secrets.savedPassword);
    }

    private static CameraProfile profile() {
        return new CameraProfile(
                "profile-1",
                "Tapo C210",
                "device-1",
                "192.168.1.20",
                2020,
                554,
                "old-user",
                StreamQuality.HIGH,
                Instant.parse("2026-08-24T12:00:00Z"));
    }

    private static final class CapturingProfileRepository implements CameraProfileRepository {
        private CameraProfile saved;

        @Override
        public void save(CameraProfile profile) {
            saved = profile;
        }

        @Override
        public List<CameraProfile> list() throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String profileId) throws SQLException {
            throw new UnsupportedOperationException();
        }
    }

    private static final class CapturingSecretStore implements SecretStore {
        private String savedPassword;

        @Override
        public void save(String profileId, String password) {
            savedPassword = password;
        }

        @Override
        public Optional<String> load(String profileId) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String profileId) throws SQLException {
            throw new UnsupportedOperationException();
        }
    }
}
