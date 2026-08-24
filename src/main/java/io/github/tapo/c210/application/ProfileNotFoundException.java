package io.github.tapo.c210.application;

/** Indicates that a requested saved camera profile is unavailable. */
public class ProfileNotFoundException extends Exception {
    public ProfileNotFoundException(String profileId) {
        super("Saved camera profile was not found: " + profileId);
    }
}
