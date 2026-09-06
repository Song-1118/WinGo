package com.wingo.view;

import com.wingo.model.AppItem;
import com.wingo.model.WingetPackage;
import com.wingo.viewmodel.InstallFlowViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class MainView extends VBox {

    private final InstallFlowViewModel vm;

    public MainView(List<AppItem> apps, InstallFlowViewModel vm) {
        this.vm = vm;

        setSpacing(12);
        setPadding(new Insets(16));
        setStyle("-fx-background-color: #f5f6fa;");

        // ===== 顶部标题栏 =====
        Label title = new Label("WinGo");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#2c3e50"));

        Label subtitle = new Label("现代软件安装器 · Winget GUI");
        subtitle.setFont(Font.font("System", 14));
        subtitle.setTextFill(Color.web("#7f8c8d"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label();
        statusBadge.textProperty().bind(vm.statusProperty().asString());
        statusBadge.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-background-radius: 15; -fx-padding: 4 12;");

        HBox titleBox = new HBox(10, title, subtitle, spacer, statusBadge);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // ===== 状态消息 =====
        Label messageLabel = new Label();
        messageLabel.textProperty().bind(vm.messageProperty());
        messageLabel.setStyle("-fx-font-size: 14px; -fx-padding: 8 0;");
        messageLabel.setTextFill(Color.web("#34495e"));

        // ===== 左侧：应用列表 =====
        Label appTitle = new Label("应用列表");
        appTitle.setFont(Font.font("System", FontWeight.SEMI_BOLD, 16));

        ListView<AppItem> appList = new ListView<>();
        appList.setItems(FXCollections.observableArrayList(apps));
        appList.setPrefHeight(260);
        appList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(AppItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.name());
                    Label strategyLabel = new Label(item.strategy());
                    strategyLabel.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;" +
                            "-fx-background-radius: 10; -fx-padding: 2 8;");
                    setGraphic(strategyLabel);
                }
            }
        });

        Button installBtn = new Button("安装所选应用");
        installBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-padding: 8 16; -fx-font-size: 14;");
        installBtn.setMaxWidth(Double.MAX_VALUE);
        installBtn.setOnAction(e -> {
            AppItem selected = appList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                vm.install(selected);
            }
        });

        VBox leftBox = new VBox(10, appTitle, appList, installBtn);
        leftBox.setPrefWidth(320);
        VBox.setVgrow(appList, Priority.ALWAYS);

        // ===== 右侧：候选包 =====
        Label candidateTitle = new Label("候选包");
        candidateTitle.setFont(Font.font("System", FontWeight.SEMI_BOLD, 16));

        TableView<WingetPackage> candidateTable = new TableView<>();
        candidateTable.setItems(vm.candidatesProperty());
        candidateTable.setPrefHeight(260);

        TableColumn<WingetPackage, String> colName = new TableColumn<>("名称");
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));

        TableColumn<WingetPackage, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().id()));

        TableColumn<WingetPackage, String> colVersion = new TableColumn<>("版本");
        colVersion.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().version()));

        candidateTable.getColumns().addAll(colName, colId, colVersion);

        colName.prefWidthProperty().bind(candidateTable.widthProperty().multiply(0.3));
        colId.prefWidthProperty().bind(candidateTable.widthProperty().multiply(0.5));
        colVersion.prefWidthProperty().bind(candidateTable.widthProperty().multiply(0.2));

        CheckBox rememberBox = new CheckBox("记住此选择");

        Button confirmBtn = new Button("确认安装");
        confirmBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-padding: 8 16; -fx-font-size: 14;");
        confirmBtn.setMaxWidth(Double.MAX_VALUE);
        confirmBtn.setOnAction(e -> {
            WingetPackage selected = candidateTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                vm.confirmSelection(selected, rememberBox.isSelected());
            }
        });

        // 仅当处于“等待选择”状态时才显示右侧区域
        var waiting = vm.statusProperty()
                .isEqualTo(InstallFlowViewModel.InstallStatus.WAITING_CHOICE);
        candidateTable.visibleProperty().bind(waiting);
        candidateTable.managedProperty().bind(waiting);
        rememberBox.visibleProperty().bind(waiting);
        rememberBox.managedProperty().bind(waiting);
        confirmBtn.visibleProperty().bind(waiting);
        confirmBtn.managedProperty().bind(waiting);

        VBox rightBox = new VBox(10, candidateTitle, candidateTable, rememberBox, confirmBtn);
        rightBox.setPrefWidth(520);
        VBox.setVgrow(candidateTable, Priority.ALWAYS);

        // ===== 左右分栏 =====
        HBox content = new HBox(16, leftBox, rightBox);
        HBox.setHgrow(leftBox, Priority.NEVER);
        HBox.setHgrow(rightBox, Priority.ALWAYS);
        VBox.setVgrow(content, Priority.ALWAYS);

        getChildren().addAll(titleBox, messageLabel, content);
    }
}