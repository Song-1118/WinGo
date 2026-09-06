package com.wingo.viewmodel;

import com.wingo.model.AppItem;
import com.wingo.model.WingetPackage;
import com.wingo.service.ConfigService;
import com.wingo.service.WingetService;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InstallFlowViewModel {

    private static final Logger LOGGER = LogManager.getLogger(InstallFlowViewModel.class);

    public enum InstallStatus {
        IDLE, CHECKING, SEARCHING, WAITING_CHOICE, INSTALLING, SUCCESS, FAILED, WINGET_NOT_FOUND
    }

    private final WingetService wingetService;
    private final ConfigService configService;
    private final ObjectProperty<InstallStatus> status = new SimpleObjectProperty<>(InstallStatus.IDLE);
    private final StringProperty message = new SimpleStringProperty("准备就绪");
    private final ObservableList<WingetPackage> candidates = FXCollections.observableArrayList();
    private AppItem currentApp;

    public InstallFlowViewModel(WingetService wingetService, ConfigService configService) {
        this.wingetService = wingetService;
        this.configService = configService;
    }

    public ObjectProperty<InstallStatus> statusProperty() {
        return status;
    }

    public StringProperty messageProperty() {
        return message;
    }

    public ObservableList<WingetPackage> candidatesProperty() {
        return candidates;
    }

    public InstallStatus getStatus() {
        return status.get();
    }

    public void install(AppItem app) {
        this.currentApp = app;
        updateStatus(InstallStatus.CHECKING);
        message.set("正在检查 winget 环境...");

        wingetService.isAvailableAsync().thenAccept(available -> Platform.runLater(() -> {
            if (!available) {
                updateStatus(InstallStatus.WINGET_NOT_FOUND);
                message.set("未检测到 winget，请先安装 WinGet");
                return;
            }
            if ("fixed".equals(app.strategy())) {
                installById(app.primaryId());
            } else if ("search".equals(app.strategy())) {
                startSearch(app);
            }
        }));
    }

    public void confirmSelection(WingetPackage selected, boolean remember) {
        if (remember) {
            configService.saveRememberedChoiceAsync(currentApp.name(), selected.id());
        }
        message.set("已选择: " + selected.name());
        installById(selected.id());
    }

    private void startSearch(AppItem app) {
        updateStatus(InstallStatus.SEARCHING);
        configService.getRememberedChoiceAsync(app.name())
                .thenAccept(rememberedId -> Platform.runLater(() -> {
                    if (rememberedId != null) {
                        message.set("使用记住的选择: " + rememberedId);
                        installById(rememberedId);
                    } else {
                        wingetService.searchAsync(app.keyword())
                                .thenAccept(list -> Platform.runLater(() -> {
                                    candidates.setAll(list);
                                    updateStatus(InstallStatus.WAITING_CHOICE);
                                    message.set("请选择要安装的包");
                                }));
                    }
                }));
    }

    private void installById(String id) {
        updateStatus(InstallStatus.INSTALLING);
        message.set("正在安装 " + id + " ...");

        wingetService.installAsync(id).thenAccept(exitCode -> Platform.runLater(() -> {
            if (exitCode == 0) {
                updateStatus(InstallStatus.SUCCESS);
                message.set("安装成功: " + id);
            } else {
                updateStatus(InstallStatus.FAILED);
                message.set("安装失败，退出码: " + exitCode);
            }
        }));
    }

    private void updateStatus(InstallStatus newStatus) {
        LOGGER.info("状态: {} -> {}", status.get(), newStatus);
        status.set(newStatus);
    }
}