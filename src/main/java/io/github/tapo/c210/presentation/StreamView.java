package io.github.tapo.c210.presentation;

import io.github.tapo.c210.domain.CameraCapabilities;
import io.github.tapo.c210.domain.MotionEvent;
import io.github.tapo.c210.domain.PtzDirection;
import io.github.tapo.c210.domain.StreamQuality;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** JavaFX live-stream screen backed by a VLCJ ImageView video surface. */
public final class StreamView {
    private final BorderPane root;
    private final ImageView imageView;
    private final Label status;
    private final Label capabilityStatus;
    private final Label motionStatus;
    private final Label recordingStatus;
    private final Label talkbackStatus;
    private final ComboBox<StreamQuality> streamQualitySelector;
    private final Button[] ptzButtons;
    private final Button recordingButton;
    private Consumer<PtzDirection> ptzAction = direction -> { };
    private Consumer<StreamQuality> streamQualityAction = quality -> { };
    private Runnable ptzStopAction = () -> { };
    private Runnable recordingStartAction = () -> { };
    private Runnable recordingStopAction = () -> { };
    private boolean streamQualityConfigured;
    private boolean updatingStreamQuality;
    private boolean recording;

    public StreamView(Runnable onDisconnect) {
        Objects.requireNonNull(onDisconnect, "onDisconnect must not be null");

        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(760);
        imageView.setFitHeight(430);
        var videoArea = new StackPane(imageView);
        videoArea.setMinSize(640, 360);
        videoArea.setStyle("-fx-background-color: #101010;");

        status = new Label("接続中…");
        status.setMaxWidth(Double.MAX_VALUE);

        capabilityStatus = new Label("能力確認中…");
        motionStatus = new Label("動体検知: 確認中…");
        recordingStatus = new Label("ローカル録画: Adapter確認待ち");
        talkbackStatus = new Label("音声通話: 実機Adapter確認待ち");

        var left = new Button("←");
        var right = new Button("→");
        var up = new Button("↑");
        var down = new Button("↓");
        var stop = new Button("停止");
        ptzButtons = new Button[] {left, right, up, down, stop};
        for (var button : ptzButtons) {
            button.setDisable(true);
        }
        left.setOnAction(event -> ptzAction.accept(PtzDirection.PAN_LEFT));
        right.setOnAction(event -> ptzAction.accept(PtzDirection.PAN_RIGHT));
        up.setOnAction(event -> ptzAction.accept(PtzDirection.TILT_UP));
        down.setOnAction(event -> ptzAction.accept(PtzDirection.TILT_DOWN));
        stop.setOnAction(event -> ptzStopAction.run());

        streamQualitySelector = new ComboBox<>(
                FXCollections.observableArrayList(StreamQuality.values()));
        streamQualitySelector.setValue(StreamQuality.HIGH);
        streamQualitySelector.setDisable(true);
        streamQualitySelector.setOnAction(event -> {
            var quality = streamQualitySelector.getValue();
            if (!updatingStreamQuality && quality != null) {
                streamQualityAction.accept(quality);
            }
        });

        var ptzPad = new GridPane();
        ptzPad.setHgap(6);
        ptzPad.setVgap(6);
        ptzPad.add(up, 1, 0);
        ptzPad.add(left, 0, 1);
        ptzPad.add(stop, 1, 1);
        ptzPad.add(right, 2, 1);
        ptzPad.add(down, 1, 2);
        recordingButton = new Button("録画開始");
        var controls = new VBox(
                8,
                new Label("カメラ操作"),
                capabilityStatus,
                new Label("ストリーム画質"),
                streamQualitySelector,
                new Label("PTZ"),
                ptzPad,
                motionStatus,
                recordingStatus,
                recordingButton,
                talkbackStatus);
        recordingButton.setDisable(true);
        recordingButton.setOnAction(event -> {
            if (recording) {
                recordingStopAction.run();
            } else {
                recordingStartAction.run();
            }
        });
        controls.setPadding(new Insets(8, 12, 8, 0));

        var disconnect = new Button("切断");
        disconnect.setOnAction(event -> onDisconnect.run());
        var actions = new HBox(8, disconnect);
        actions.setAlignment(Pos.CENTER_RIGHT);

        var header = new VBox(4, new Label("ライブ映像"), status);
        root = new BorderPane();
        root.setTop(header);
        root.setCenter(videoArea);
        root.setRight(controls);
        root.setBottom(actions);
        BorderPane.setMargin(header, new Insets(16, 16, 8, 16));
        BorderPane.setMargin(videoArea, new Insets(8, 16, 8, 16));
        BorderPane.setMargin(controls, new Insets(8, 16, 8, 0));
        BorderPane.setMargin(actions, new Insets(8, 16, 16, 16));
    }

    public Parent root() {
        return root;
    }

    public ImageView imageView() {
        return imageView;
    }

    public void showPlaying(String displayName, StreamQuality quality) {
        selectStreamQuality(quality);
        status.setText("再生中: %s · %s".formatted(displayName, quality == StreamQuality.HIGH
                ? "高画質"
                : "低画質"));
    }

    public void showWarning(String message) {
        status.setText("再生中（警告）: " + message);
    }

    public void setStreamQualityAction(Consumer<StreamQuality> onChange) {
        streamQualityAction = Objects.requireNonNull(onChange, "onChange must not be null");
        streamQualityConfigured = true;
        setStreamQualityEnabled(true);
    }

    public void showQualityChanging(StreamQuality quality) {
        selectStreamQuality(quality);
        streamQualitySelector.setDisable(true);
        status.setText("画質を切り替え中…");
    }

    public void showQualityChangeError(StreamQuality fallback) {
        selectStreamQuality(fallback);
        setStreamQualityEnabled(true);
        status.setText("画質変更失敗（現在の画質で再生中）");
    }

    public void setStreamQualityEnabled(boolean enabled) {
        streamQualitySelector.setDisable(!streamQualityConfigured || !enabled);
    }

    public void setPtzActions(Consumer<PtzDirection> onMove, Runnable onStop) {
        ptzAction = Objects.requireNonNull(onMove, "onMove must not be null");
        ptzStopAction = Objects.requireNonNull(onStop, "onStop must not be null");
    }

    public void setRecordingActions(Runnable onStart, Runnable onStop) {
        recordingStartAction = Objects.requireNonNull(onStart, "onStart must not be null");
        recordingStopAction = Objects.requireNonNull(onStop, "onStop must not be null");
        recordingButton.setDisable(false);
    }

    public void showRecordingAvailable() {
        if (!recording) {
            recordingStatus.setText("ローカル録画: 利用可能（映像のみ・開始時に保存先を選択）");
        }
    }

    public void showRecordingStarted(Path output) {
        Objects.requireNonNull(output, "output must not be null");
        recording = true;
        recordingButton.setText("録画停止");
        streamQualitySelector.setDisable(true);
        recordingStatus.setText("録画中（映像のみ）: " + output.getFileName());
    }

    public void showRecordingStopped() {
        recording = false;
        recordingButton.setText("録画開始");
        setStreamQualityEnabled(true);
        showRecordingAvailable();
    }

    public void showRecordingError() {
        recording = false;
        recordingButton.setText("録画開始");
        setStreamQualityEnabled(true);
        recordingStatus.setText("ローカル録画: 開始できませんでした");
    }

    public void showCapabilities(CameraCapabilities capabilities) {
        Objects.requireNonNull(capabilities, "capabilities must not be null");
        capabilityStatus.setText("能力取得完了");
        setPtzEnabled(capabilities.ptz());
        motionStatus.setText("動体検知: " + supportedText(capabilities.motionEvents()));
        if (recordingButton.isDisabled()) {
            recordingStatus.setText("ローカル録画: " + supportedText(capabilities.localRecording()));
        } else if (!recording) {
            showRecordingAvailable();
        }
        talkbackStatus.setText("音声通話: " + supportedText(capabilities.talkback()));
    }

    public void showCapabilitiesUnavailable() {
        capabilityStatus.setText("能力取得失敗（映像は再生中）");
        setPtzEnabled(false);
        motionStatus.setText("動体検知: 利用不可");
        if (recordingButton.isDisabled()) {
            recordingStatus.setText("ローカル録画: 未確認");
        }
        talkbackStatus.setText("音声通話: 未確認");
    }

    public void showMotionEvent(MotionEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        motionStatus.setText("動体検知: %s (%s)".formatted(event.type(), event.occurredAt()));
    }

    private void setPtzEnabled(boolean enabled) {
        for (var button : ptzButtons) {
            button.setDisable(!enabled);
        }
    }

    private void selectStreamQuality(StreamQuality quality) {
        Objects.requireNonNull(quality, "quality must not be null");
        updatingStreamQuality = true;
        try {
            streamQualitySelector.setValue(quality);
        } finally {
            updatingStreamQuality = false;
        }
    }

    private static String supportedText(boolean supported) {
        return supported ? "利用可能" : "利用不可";
    }
}
