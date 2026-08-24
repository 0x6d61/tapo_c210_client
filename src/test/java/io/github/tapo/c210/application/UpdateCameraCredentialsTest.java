package io.github.tapo.c210.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.tapo.c210.application.port.CameraProfileRepository;
import io.github.tapo.c210.application.port.SecretStore;
import io.github.tapo.c210.domain.CameraProfile;
import io.github.tapo.c210.domain.StreamQuality;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateCameraCredentialsTest {
    @Test
    void updatesUsernameAndPasswordTogether() throws Exception {
        var repository = new CapturingProfileRepository();
        var secrets = new CapturingSecretStore();

        new UpdateCameraCredentials(repository, secrets).execute(
                profile(), "new-user", "new-password");

        assertEquals("new-user", repository.saved.username());
        assertEquals("new-password", secrets.savedPassword);
    }

    @Test
    void leavesTheExistingPasswordWhenThePasswordFieldIsBlank() throws Exception {
        var repository = new CapturingProfileRepository();
        var secrets = new CapturingSecretStore();

        new UpdateCameraCredentials(repository, secrets).execute(
                profile(), "new-user", " ");

        assertEquals("new-user", repository.saved.username());
        assertFalse(secrets.wasSaved);
    }

    @Test
    void rejectsABlankUsername() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UpdateCameraCredentials(
                        new CapturingProfileRepository(), new CapturingSecretStore())
                        .execute(profile(), " ", "new-password"));
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
        private boolean wasSaved;

        @Override
        public void save(String profileId, String password) {
            savedPassword = password;
            wasSaved = true;
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
