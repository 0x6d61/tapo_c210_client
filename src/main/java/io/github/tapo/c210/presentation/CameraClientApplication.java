package io.github.tapo.c210.presentation;

import io.github.tapo.c210.application.ListSavedProfiles;
import io.github.tapo.c210.application.DiscoverCameras;
import io.github.tapo.c210.application.ValidatedConnectionForm;
import io.github.tapo.c210.discovery.WsDiscoveryClient;
import io.github.tapo.c210.domain.CameraDevice;
import io.github.tapo.c210.domain.CameraProfile;
import io.github.tapo.c210.persistence.SqliteDatabase;
import io.github.tapo.c210.persistence.SqliteProfileRepository;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

/** JavaFX entry point for the C210 connection flow. */
public final class CameraClientApplication extends Application {
    private Stage stage;
    private SqliteDatabase database;
    private Task<List<CameraDevice>> discoveryTask;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        database = SqliteDatabase.open(defaultDatabasePath());
        stage.setTitle("Tapo C210 Client");
        showConnectionSelection(new ListSavedProfiles(new SqliteProfileRepository(database)).execute());
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (discoveryTask != null) {
            discoveryTask.cancel();
        }
        if (database != null) {
            database.close();
        }
    }

    private void showConnectionSelection(List<CameraProfile> profiles) {
        var view = new ConnectionSelectionView(
                profiles,
                this::showProfileNotWiredMessage,
                this::showDiscovery,
                this::showConnectionForm);
        stage.setScene(new Scene(view.root(), 720, 480));
    }

    private void showConnectionForm() {
        var view = new ConnectionFormView(
                () -> showConnectionSelection(loadProfiles()),
                this::showValidatedFormMessage);
        stage.setScene(new Scene(view.root(), 620, 480));
    }

    private void showConnectionForm(CameraDevice device) {
        var view = new ConnectionFormView(
                device.host(),
                device.onvifPort(),
                device.rtspPort(),
                () -> showConnectionSelection(loadProfiles()),
                this::showValidatedFormMessage);
        stage.setScene(new Scene(view.root(), 620, 480));
    }

    private void showDiscovery() {
        var view = new CameraDiscoveryView();
        view.setOnCancel(() -> {
            if (discoveryTask != null) {
                discoveryTask.cancel();
            }
            showConnectionSelection(loadProfiles());
        });
        view.setOnSelect(device -> {
            if (discoveryTask != null) {
                discoveryTask.cancel();
            }
            showConnectionForm(device);
        });
        stage.setScene(new Scene(view.root(), 720, 480));

        var task = new Task<List<CameraDevice>>() {
            @Override
            protected List<CameraDevice> call() throws Exception {
                return new DiscoverCameras(new WsDiscoveryClient()).execute(
                        Duration.ofSeconds(4), this::isCancelled);
            }
        };
        discoveryTask = task;
        task.setOnSucceeded(event -> view.showResults(task.getValue()));
        task.setOnFailed(event -> view.showError());
        task.setOnCancelled(event -> view.showCancelled());

        var worker = new Thread(task, "tapo-c210-discovery");
        worker.setDaemon(true);
        worker.start();
    }

    private List<CameraProfile> loadProfiles() {
        try {
            return new ListSavedProfiles(new SqliteProfileRepository(database)).execute();
        } catch (Exception exception) {
            showInfo("保存済み接続", "保存済みプロファイルを読み込めませんでした。");
            return List.of();
        }
    }

    private static Path defaultDatabasePath() {
        var osName = System.getProperty("os.name", "").toLowerCase();
        var home = Paths.get(System.getProperty("user.home"));
        if (osName.contains("win")) {
            var appData = System.getenv("APPDATA");
            return (appData == null || appData.isBlank()
                    ? home.resolve("AppData").resolve("Roaming")
                    : Paths.get(appData))
                    .resolve("TapoC210Client")
                    .resolve("camera.db");
        }
        if (osName.contains("mac")) {
            return home.resolve("Library").resolve("Application Support")
                    .resolve("TapoC210Client").resolve("camera.db");
        }
        return home.resolve(".local").resolve("share")
                .resolve("tapo-c210-client").resolve("camera.db");
    }

    private void showProfileNotWiredMessage(CameraProfile profile) {
        showInfo("接続準備", "%sを選択しました。RTSP接続Adapterを次の実装で接続します。"
                .formatted(profile.displayName()));
    }

    private void showValidatedFormMessage(ValidatedConnectionForm form) {
        showInfo("入力確認", "%s:%d の接続情報を受け付けました。"
                .formatted(form.host(), form.rtspPort()));
    }

    private void showInfo(String title, String message) {
        var alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
