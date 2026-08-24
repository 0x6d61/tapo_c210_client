package io.github.tapo.c210.presentation;

import io.github.tapo.c210.domain.StreamQuality;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** JavaFX live-stream screen backed by a VLCJ ImageView video surface. */
public final class StreamView {
    private final BorderPane root;
    private final ImageView imageView;
    private final Label status;

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
        var disconnect = new Button("切断");
        disconnect.setOnAction(event -> onDisconnect.run());
        var actions = new HBox(8, disconnect);
        actions.setAlignment(Pos.CENTER_RIGHT);

        var header = new VBox(4, new Label("ライブ映像"), status);
        root = new BorderPane();
        root.setTop(header);
        root.setCenter(videoArea);
        root.setBottom(actions);
        BorderPane.setMargin(header, new Insets(16, 16, 8, 16));
        BorderPane.setMargin(videoArea, new Insets(8, 16, 8, 16));
        BorderPane.setMargin(actions, new Insets(8, 16, 16, 16));
    }

    public Parent root() {
        return root;
    }

    public ImageView imageView() {
        return imageView;
    }

    public void showPlaying(String displayName, StreamQuality quality) {
        status.setText("再生中: %s · %s".formatted(displayName, quality == StreamQuality.HIGH
                ? "高画質"
                : "低画質"));
    }

    public void showWarning(String message) {
        status.setText("再生中（警告）: " + message);
    }
}
