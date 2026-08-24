package io.github.tapo.c210.presentation;

import io.github.tapo.c210.domain.CameraDevice;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** JavaFX view for asynchronous ONVIF camera discovery. */
public final class CameraDiscoveryView {
    private final BorderPane root;
    private final ListView<CameraDevice> devices;
    private final Label status;
    private final ProgressIndicator progress;
    private final Button select;
    private Consumer<CameraDevice> onSelect = device -> {
    };
    private Runnable onCancel = () -> {
    };

    public CameraDiscoveryView() {
        status = new Label();
        progress = new ProgressIndicator();
        progress.setPrefSize(24, 24);
        devices = new ListView<>();
        devices.setPlaceholder(new Label("検出結果はありません"));
        devices.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(CameraDevice device, boolean empty) {
                super.updateItem(device, empty);
                setText(empty || device == null
                        ? null
                        : "%s%n%s · ONVIF %d".formatted(
                                device.model() == null ? "ONVIFカメラ" : device.model(),
                                device.host(),
                                device.onvifPort()));
            }
        });
        select = new Button("このカメラを選択");
        select.setDisable(true);
        devices.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, current) -> select.setDisable(current == null));
        select.setOnAction(event -> {
            var selected = devices.getSelectionModel().getSelectedItem();
            if (selected != null) {
                onSelect.accept(selected);
            }
        });
        var cancel = new Button("キャンセル");
        cancel.setOnAction(event -> onCancel.run());

        var header = new HBox(10, new Label("カメラを検索中"), progress);
        var actions = new HBox(8, select, cancel);
        root = new BorderPane();
        root.setTop(header);
        root.setCenter(devices);
        root.setBottom(new VBox(8, status, actions));
        root.setPadding(new Insets(20));
        showSearching();
    }

    public void setOnSelect(Consumer<CameraDevice> onSelect) {
        this.onSelect = Objects.requireNonNull(onSelect, "onSelect must not be null");
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = Objects.requireNonNull(onCancel, "onCancel must not be null");
    }

    public void showSearching() {
        progress.setVisible(true);
        select.setDisable(true);
        status.setText("同一LAN上のONVIFカメラを検索しています…");
    }

    public void showResults(List<CameraDevice> results) {
        devices.setItems(FXCollections.observableArrayList(results));
        progress.setVisible(false);
        status.setText("%d台のカメラを検出しました。".formatted(results.size()));
        select.setDisable(true);
    }

    public void showError() {
        progress.setVisible(false);
        status.setText("カメラを検出できませんでした。IPアドレスを手入力できます。");
        select.setDisable(true);
    }

    public void showCancelled() {
        progress.setVisible(false);
        status.setText("カメラ検索をキャンセルしました。");
        select.setDisable(true);
    }

    public Parent root() {
        return root;
    }
}
