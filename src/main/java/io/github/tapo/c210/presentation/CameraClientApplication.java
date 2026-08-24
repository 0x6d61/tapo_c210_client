package io.github.tapo.c210.presentation;

import io.github.tapo.c210.application.CameraConnectionException;
import io.github.tapo.c210.application.CameraControlException;
import io.github.tapo.c210.application.CameraCredentials;
import io.github.tapo.c210.application.ConnectWithCredentials;
import io.github.tapo.c210.application.ConnectWithProfile;
import io.github.tapo.c210.application.DiscoverCameras;
import io.github.tapo.c210.application.ConnectedCamera;
import io.github.tapo.c210.application.ListSavedProfiles;
import io.github.tapo.c210.application.MoveCamera;
import io.github.tapo.c210.application.StopCameraMovement;
import io.github.tapo.c210.application.ValidatedConnectionForm;
import io.github.tapo.c210.application.port.RtspConnector;
import io.github.tapo.c210.discovery.WsDiscoveryClient;
import io.github.tapo.c210.domain.CameraCapabilities;
import io.github.tapo.c210.domain.CameraDevice;
import io.github.tapo.c210.domain.CameraProfile;
import io.github.tapo.c210.domain.PtzCommand;
import io.github.tapo.c210.domain.PtzDirection;
import io.github.tapo.c210.persistence.SqliteDatabase;
import io.github.tapo.c210.persistence.SqliteProfileRepository;
import io.github.tapo.c210.persistence.SqliteSecretStore;
import io.github.tapo.c210.onvif.OnvifCameraAdapter;
import io.github.tapo.c210.streaming.VlcjRtspConnector;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
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
    private Task<CameraCapabilities> capabilityTask;
    private VlcjRtspConnector rtspConnector;
    private ConnectedCamera activeSession;
    private OnvifCameraAdapter onvifAdapter;
    private CameraCapabilities capabilities = CameraCapabilities.none();

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
        cancelCapabilities();
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
            var password = new SqliteSecretStore(database).load(profile.id())
                    .orElseThrow(() -> new IllegalStateException("saved camera password is missing"));
            var connector = openRtspConnector();
            RtspConnector adapter = request -> connector.connect(request, view.imageView());
            activeSession = new ConnectWithProfile(
                    new SqliteProfileRepository(database),
                    new SqliteSecretStore(database),
                    adapter)
                    .execute(profile.id());
            view.showPlaying(profile.displayName(), profile.streamQuality());
            preparePtz(view);
            startCapabilityLoading(
                    view,
                    profileDevice(profile),
                    new CameraCredentials(profile.username(), password));
        } catch (Exception exception) {
            cancelCapabilities();
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
            preparePtz(view);
            startCapabilityLoading(
                    view,
                    discoveredDevice.orElseGet(() -> manualDevice(form)),
                    new CameraCredentials(form.username(), form.password()));
        } catch (CameraConnectionException exception) {
            cancelCapabilities();
            showInfo("接続失敗", "RTSPストリームを開始できませんでした。接続先とカメラアカウントを確認してください。");
            showConnectionSelection(loadProfiles());
        } catch (Exception exception) {
            cancelCapabilities();
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
        cancelCapabilities();
        closeActiveSession();
        showConnectionSelection(loadProfiles());
    }

    private void preparePtz(StreamView view) {
        view.setPtzActions(
                direction -> movePtz(direction),
                this::stopPtz);
    }

    private void startCapabilityLoading(
            StreamView view, CameraDevice camera, CameraCredentials credentials) {
        var task = new Task<CameraCapabilities>() {
            @Override
            protected CameraCapabilities call() throws Exception {
                var adapter = OnvifCameraAdapter.connect(camera, credentials, Duration.ofSeconds(8));
                onvifAdapter = adapter;
                return adapter.load();
            }
        };
        capabilityTask = task;
        task.setOnSucceeded(event -> {
            if (capabilityTask == task) {
                capabilities = task.getValue();
                view.showCapabilities(capabilities);
            }
        });
        task.setOnFailed(event -> {
            if (capabilityTask == task) {
                view.showCapabilitiesUnavailable();
            }
        });
        task.setOnCancelled(event -> {
            if (capabilityTask == task) {
                view.showCapabilitiesUnavailable();
            }
        });

        var worker = new Thread(task, "tapo-c210-capabilities");
        worker.setDaemon(true);
        worker.start();
    }

    private void movePtz(PtzDirection direction) {
        var adapter = onvifAdapter;
        if (adapter == null) {
            return;
        }
        var task = new Task<Void>() {
            @Override
            protected Void call() throws CameraControlException {
                new MoveCamera(capabilities, adapter).execute(
                        new PtzCommand(direction, 0.5, Duration.ofMillis(500)));
                return null;
            }
        };
        runControlTask(task);
    }

    private void stopPtz() {
        var adapter = onvifAdapter;
        if (adapter == null) {
            return;
        }
        var task = new Task<Void>() {
            @Override
            protected Void call() throws CameraControlException {
                new StopCameraMovement(capabilities, adapter).execute();
                return null;
            }
        };
        runControlTask(task);
    }

    private void runControlTask(Task<Void> task) {
        var worker = new Thread(task, "tapo-c210-control");
        worker.setDaemon(true);
        worker.start();
    }

    private void cancelCapabilities() {
        if (capabilityTask != null) {
            capabilityTask.cancel();
            capabilityTask = null;
        }
        if (onvifAdapter != null) {
            onvifAdapter.close();
            onvifAdapter = null;
        }
        capabilities = CameraCapabilities.none();
    }

    private static CameraDevice profileDevice(CameraProfile profile) {
        return new CameraDevice(
                profile.deviceId() == null ? profile.id() : profile.deviceId(),
                profile.host(),
                profile.onvifPort(),
                profile.rtspPort(),
                URI.create("http://%s:%d/onvif/device_service"
                        .formatted(profile.host(), profile.onvifPort())),
                null,
                null,
                null);
    }

    private static CameraDevice manualDevice(ValidatedConnectionForm form) {
        return new CameraDevice(
                "manual:%s:%d".formatted(form.host(), form.onvifPort()),
                form.host(),
                form.onvifPort(),
                form.rtspPort(),
                URI.create("http://%s:%d/onvif/device_service"
                        .formatted(form.host(), form.onvifPort())),
                null,
                null,
                null);
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
