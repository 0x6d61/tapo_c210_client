package io.github.tapo.c210.application;

/** Indicates that a saved profile has no password in the local secret store. */
public class MissingCameraPasswordException extends Exception {
    public MissingCameraPasswordException(String profileId) {
        super("No stored password is available for camera profile: " + profileId);
    }
}
