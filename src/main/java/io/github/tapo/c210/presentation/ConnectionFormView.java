package io.github.tapo.c210.presentation;

import io.github.tapo.c210.application.ConnectionFormInput;
import io.github.tapo.c210.application.ConnectionFormValidator;
import io.github.tapo.c210.application.ValidatedConnectionForm;
import io.github.tapo.c210.domain.StreamQuality;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** JavaFX form for manually entering a camera connection. */
public final class ConnectionFormView {
    private final VBox root;

    public ConnectionFormView(
            Runnable onBack,
            Consumer<ValidatedConnectionForm> onConnect) {
        Objects.requireNonNull(onBack, "onBack must not be null");
        Objects.requireNonNull(onConnect, "onConnect must not be null");

        var host = new TextField();
        host.setPromptText("192.168.1.20");
        var onvifPort = new TextField("2020");
        var rtspPort = new TextField("554");
        var username = new TextField();
        var password = new PasswordField();
        var streamQuality = new ComboBox<StreamQuality>(
                FXCollections.observableArrayList(StreamQuality.values()));
        streamQuality.setValue(StreamQuality.HIGH);
        var remember = new CheckBox("この接続を記憶する");
        var error = new Label();
        error.setWrapText(true);

        var fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(10);
        fields.addRow(0, new Label("IPアドレス"), host);
        fields.addRow(1, new Label("ONVIFポート"), onvifPort);
        fields.addRow(2, new Label("RTSPポート"), rtspPort);
        fields.addRow(3, new Label("ユーザー名"), username);
        fields.addRow(4, new Label("パスワード"), password);
        fields.addRow(5, new Label("ストリーム"), streamQuality);

        var back = new Button("戻る");
        back.setOnAction(event -> onBack.run());
        var connect = new Button("接続する");
        connect.setDefaultButton(true);
        connect.setOnAction(event -> {
            var validation = new ConnectionFormValidator().validate(new ConnectionFormInput(
                    host.getText(),
                    onvifPort.getText(),
                    rtspPort.getText(),
                    username.getText(),
                    password.getText(),
                    streamQuality.getValue(),
                    remember.isSelected()));
            error.setText(String.join("\n", validation.errors()));
            validation.value().ifPresent(onConnect);
        });

        var actions = new HBox(8, back, connect);
        actions.setAlignment(Pos.CENTER_RIGHT);
        root = new VBox(
                12,
                new Label("カメラに接続"),
                fields,
                remember,
                error,
                actions);
        root.setPadding(new Insets(20));
    }

    public Parent root() {
        return root;
    }
}
