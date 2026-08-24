package io.github.tapo.c210.application;

import java.util.Objects;

/** Camera-account credentials used only while opening a connection. */
public final class CameraCredentials {
    private final String username;
    private final String password;

    public CameraCredentials(String username, String password) {
        this.username = requireNonBlank(username, "username");
        this.password = requireNonBlank(password, "password");
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    @Override
    public String toString() {
        return "CameraCredentials[username='%s', password=<redacted>]".formatted(username);
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
