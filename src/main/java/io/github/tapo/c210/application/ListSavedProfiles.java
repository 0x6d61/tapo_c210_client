package io.github.tapo.c210.application;

import io.github.tapo.c210.application.port.CameraProfileRepository;
import io.github.tapo.c210.domain.CameraProfile;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** Application use case for displaying saved camera profiles. */
public final class ListSavedProfiles {
    private final CameraProfileRepository repository;

    public ListSavedProfiles(CameraProfileRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public List<CameraProfile> execute() throws SQLException {
        return repository.list();
    }
}
