package io.github.tapo.c210.presentation;

import io.github.tapo.c210.domain.CameraProfile;
import java.util.Objects;
import java.util.function.BiConsumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** JavaFX form for editing the username and password of a saved camera profile. */
public final class CameraProfileEditView {
    private final VBox root;

    public CameraProfileEditView(
            CameraProfile profile,
            Runnable onCancel,
            BiConsumer<String, String> onSave) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(onCancel, "onCancel must not be null");
        Objects.requireNonNull(onSave, "onSave must not be null");

        var username = new TextField(profile.username());
        var password = new PasswordField();
        password.setPromptText("変更しない場合は空欄");
        var error = new Label();
        error.setWrapText(true);

        var fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(10);
        fields.addRow(0, new Label("ユーザー名"), username);
        fields.addRow(1, new Label("パスワード"), password);

        var cancel = new Button("キャンセル");
        cancel.setOnAction(event -> onCancel.run());
        var save = new Button("保存");
        save.setDefaultButton(true);
        save.setOnAction(event -> {
            if (username.getText() == null || username.getText().isBlank()) {
                error.setText("ユーザー名を入力してください。");
                return;
            }
            onSave.accept(username.getText(), password.getText());
        });

        var actions = new HBox(8, cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);
        root = new VBox(
                12,
                new Label("接続先を編集"),
                new Label("%s (%s)".formatted(profile.displayName(), profile.host())),
                new Label("パスワードは表示されません。変更しない場合は空欄にしてください。"),
                fields,
                error,
                actions);
        root.setPadding(new Insets(20));
    }

    public Parent root() {
        return root;
    }
}
