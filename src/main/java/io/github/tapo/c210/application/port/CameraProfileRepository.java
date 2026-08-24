package io.github.tapo.c210.application.port;

import io.github.tapo.c210.domain.CameraProfile;
import java.sql.SQLException;
import java.util.List;

/** Persistence port for camera connection profiles. */
public interface CameraProfileRepository {
    void save(CameraProfile profile) throws SQLException;

    List<CameraProfile> list() throws SQLException;

    void delete(String profileId) throws SQLException;
}
