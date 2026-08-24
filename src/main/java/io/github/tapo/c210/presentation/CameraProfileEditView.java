package io.github.tapo.c210.presentation;

import io.github.tapo.c210.application.ConnectionFormInput;
import io.github.tapo.c210.domain.CameraProfile;
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

/** JavaFX form for editing all connection fields of a saved camera profile. */
public final class CameraProfileEditView {
    private final VBox root;

    public CameraProfileEditView(
            CameraProfile profile,
            Runnable onCancel,
            Consumer<ConnectionFormInput> onSave) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(onCancel, "onCancel must not be null");
        Objects.requireNonNull(onSave, "onSave must not be null");

        var host = new TextField(profile.host());
        var onvifPort = new TextField(Integer.toString(profile.onvifPort()));
        var rtspPort = new TextField(Integer.toString(profile.rtspPort()));
        var username = new TextField(profile.username());
        var password = new PasswordField();
        password.setPromptText("変更しない場合は空欄");
        var streamQuality = new ComboBox<StreamQuality>(
                FXCollections.observableArrayList(StreamQuality.values()));
        streamQuality.setValue(profile.streamQuality());
        var remember = new CheckBox("この接続を記憶する");
        remember.setSelected(true);
        remember.setDisable(true);
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

        var cancel = new Button("キャンセル");
        cancel.setOnAction(event -> onCancel.run());
        var save = new Button("保存");
        save.setDefaultButton(true);
        save.setOnAction(event -> {
            if (username.getText() == null || username.getText().isBlank()) {
                error.setText("ユーザー名を入力してください。");
                return;
            }
            onSave.accept(new ConnectionFormInput(
                    host.getText(),
                    onvifPort.getText(),
                    rtspPort.getText(),
                    username.getText(),
                    password.getText(),
                    streamQuality.getValue(),
                    remember.isSelected()));
        });

        var actions = new HBox(8, cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);
        root = new VBox(
                12,
                new Label("接続先を編集"),
                new Label("%s (%s)".formatted(profile.displayName(), profile.host())),
                new Label("パスワードは表示されません。変更しない場合は空欄にしてください。"),
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
