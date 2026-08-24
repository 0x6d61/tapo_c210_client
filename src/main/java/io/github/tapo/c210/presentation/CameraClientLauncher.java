package io.github.tapo.c210.presentation;

import javafx.application.Application;

/** Non-Application entry point used by executable JARs on the class path. */
public final class CameraClientLauncher {
    private CameraClientLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(CameraClientApplication.class, args);
    }
}
