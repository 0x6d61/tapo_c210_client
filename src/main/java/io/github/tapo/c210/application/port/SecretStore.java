package io.github.tapo.c210.application.port;

import java.sql.SQLException;
import java.util.Optional;

/** Persistence port for locally stored camera passwords. */
public interface SecretStore {
    void save(String profileId, String password) throws SQLException;

    Optional<String> load(String profileId) throws SQLException;

    void delete(String profileId) throws SQLException;
}
