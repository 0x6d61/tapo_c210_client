package io.github.tapo.c210.presentation;

import io.github.tapo.c210.application.CameraConnectionException;
import io.github.tapo.c210.application.ConnectWithCredentials;
import io.github.tapo.c210.application.ConnectWithProfile;
import io.github.tapo.c210.application.DiscoverCameras;
import io.github.tapo.c210.application.ConnectedCamera;
import io.github.tapo.c210.application.ListSavedProfiles;
import io.github.tapo.c210.application.ValidatedConnectionForm;
import io.github.tapo.c210.application.port.RtspConnector;
import io.github.tapo.c210.discovery.WsDiscoveryClient;
import io.github.tapo.c210.domain.CameraDevice;
import io.github.tapo.c210.domain.CameraProfile;
import io.github.tapo.c210.persistence.SqliteDatabase;
import io.github.tapo.c210.persistence.SqliteProfileRepository;
import io.github.tapo.c210.persistence.SqliteSecretStore;
import io.github.tapo.c210.streaming.VlcjRtspConnector;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
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
    private VlcjRtspConnector rtspConnector;
    private ConnectedCamera activeSession;

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
        closeActiveSession();
        if (rtspConnector != null) {
            rtspConnector.close();
        }
        if (database != null) {
            database.close();
        }
    }

    private void showConnectionSelection(List<CameraProfile> profiles) {
        var view = new ConnectionSelectionView(
                profiles,
                this::connectSavedProfile,
                this::showDiscovery,
                this::showConnectionForm);
        stage.setScene(new Scene(view.root(), 720, 480));
    }

    private void showConnectionForm() {
        var view = new ConnectionFormView(
                () -> showConnectionSelection(loadProfiles()),
                form -> connectWithCredentials(form, Optional.empty()));
        stage.setScene(new Scene(view.root(), 620, 480));
    }

    private void showConnectionForm(CameraDevice device) {
        var view = new ConnectionFormView(
                device.host(),
                device.onvifPort(),
                device.rtspPort(),
                () -> showConnectionSelection(loadProfiles()),
                form -> connectWithCredentials(form, Optional.of(device)));
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

    private void connectSavedProfile(CameraProfile profile) {
        var view = new StreamView(this::disconnectAndReturnToSelection);
        stage.setScene(new Scene(view.root(), 820, 560));
        try {
            var connector = openRtspConnector();
            RtspConnector adapter = request -> connector.connect(request, view.imageView());
            activeSession = new ConnectWithProfile(
                    new SqliteProfileRepository(database),
                    new SqliteSecretStore(database),
                    adapter)
                    .execute(profile.id());
            view.showPlaying(profile.displayName(), profile.streamQuality());
        } catch (Exception exception) {
            closeActiveSession();
            showInfo("接続失敗", "カメラに接続できませんでした。保存済みのパスワードと接続先を確認してください。");
            showConnectionSelection(loadProfiles());
        }
    }

    private void connectWithCredentials(
            ValidatedConnectionForm form, Optional<CameraDevice> discoveredDevice) {
        var view = new StreamView(this::disconnectAndReturnToSelection);
        stage.setScene(new Scene(view.root(), 820, 560));
        try {
            var connector = openRtspConnector();
            RtspConnector adapter = request -> connector.connect(request, view.imageView());
            var result = new ConnectWithCredentials(
                    new SqliteProfileRepository(database),
                    new SqliteSecretStore(database),
                    adapter)
                    .execute(form, discoveredDevice);
            activeSession = result.session();
            var displayName = result.savedProfile()
                    .map(CameraProfile::displayName)
                    .orElseGet(() -> "%s:%d".formatted(form.host(), form.rtspPort()));
            view.showPlaying(displayName, form.streamQuality());
            result.persistenceWarning().ifPresent(view::showWarning);
        } catch (CameraConnectionException exception) {
            showInfo("接続失敗", "RTSPストリームを開始できませんでした。接続先とカメラアカウントを確認してください。");
            showConnectionSelection(loadProfiles());
        } catch (Exception exception) {
            showInfo("接続失敗", "カメラに接続できませんでした。入力内容を確認してください。");
            showConnectionSelection(loadProfiles());
        }
    }

    private VlcjRtspConnector openRtspConnector() throws CameraConnectionException {
        if (rtspConnector == null) {
            rtspConnector = new VlcjRtspConnector();
        }
        return rtspConnector;
    }

    private void disconnectAndReturnToSelection() {
        closeActiveSession();
        showConnectionSelection(loadProfiles());
    }

    private void closeActiveSession() {
        if (activeSession == null) {
            return;
        }
        try {
            activeSession.close();
        } catch (Exception ignored) {
            // The UI is already leaving the stream screen; cleanup is best effort.
        } finally {
            activeSession = null;
        }
    }

    private void showInfo(String title, String message) {
        var alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
