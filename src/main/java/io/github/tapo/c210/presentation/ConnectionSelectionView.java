package io.github.tapo.c210.presentation;

import io.github.tapo.c210.domain.CameraProfile;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** JavaFX view for choosing a saved camera or starting a new connection. */
public final class ConnectionSelectionView {
    private final VBox root;

    public ConnectionSelectionView(
            List<CameraProfile> profiles,
            Consumer<CameraProfile> onProfileSelected,
            Consumer<CameraProfile> onProfileEdit,
            Runnable onDiscover,
            Runnable onManualConnect) {
        Objects.requireNonNull(profiles, "profiles must not be null");
        Objects.requireNonNull(onProfileSelected, "onProfileSelected must not be null");
        Objects.requireNonNull(onProfileEdit, "onProfileEdit must not be null");
        Objects.requireNonNull(onDiscover, "onDiscover must not be null");
        Objects.requireNonNull(onManualConnect, "onManualConnect must not be null");

        var profileList = new ListView<CameraProfile>(
                FXCollections.observableArrayList(profiles));
        profileList.setPlaceholder(new Label("保存済みカメラはありません"));
        profileList.setCellFactory(view -> new ListCell<>() {
            private final MenuItem edit = new MenuItem("編集");
            private final ContextMenu contextMenu = new ContextMenu(edit);

            {
                edit.setOnAction(event -> {
                    var selected = getItem();
                    if (selected != null) {
                        onProfileEdit.accept(selected);
                    }
                });
            }

            @Override
            protected void updateItem(CameraProfile profile, boolean empty) {
                super.updateItem(profile, empty);
                if (empty || profile == null) {
                    setText(null);
                    setContextMenu(null);
                } else {
                    setText("%s%n%s · %s · %s".formatted(
                            profile.displayName(),
                            profile.host(),
                            profile.username(),
                            profile.streamQuality().name()));
                    setContextMenu(contextMenu);
                }
            }
        });
        profileList.setPrefHeight(280);

        var connect = new Button("選択したカメラに接続");
        connect.setOnAction(event -> {
            var selected = profileList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                onProfileSelected.accept(selected);
            }
        });

        var discover = new Button("カメラを検索");
        discover.setOnAction(event -> onDiscover.run());
        var manual = new Button("手動接続");
        manual.setOnAction(event -> onManualConnect.run());

        var actions = new HBox(8, connect, discover, manual);
        root = new VBox(
                12,
                new Label("接続先を選択"),
                new Label("保存済みプロファイルにはパスワードを表示しません。カメラを右クリックすると編集できます。"),
                profileList,
                actions);
        root.setPadding(new Insets(20));
    }

    public Parent root() {
        return root;
    }
}
