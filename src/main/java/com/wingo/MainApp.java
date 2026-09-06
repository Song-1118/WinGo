package com.wingo;

import atlantafx.base.theme.PrimerLight;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wingo.model.AppItem;
import com.wingo.service.*;
import com.wingo.view.MainView;
import com.wingo.viewmodel.InstallFlowViewModel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.List;

public class MainApp extends Application{

    private WingetService wingetService;
    private ConfigService configService;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. 创建服务
        wingetService = new WingetService();
        configService = new ConfigService();
        // 2. 加载 apps.json
        ObjectMapper mapper = new ObjectMapper();
        List<AppItem> apps;
        try (InputStream in = MainApp.class.getResourceAsStream("/apps.json")) {
            apps = mapper.readValue(in, new TypeReference<>() {});
        }
        // 3. 创建 ViewModel
        InstallFlowViewModel vm = new InstallFlowViewModel(wingetService, configService);
        // 4. 创建 MainView + Scene + 主题
        MainView root = new MainView(apps, vm);
        Scene scene = new Scene(root, 800, 600);
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        primaryStage.setTitle("WinGo");
        primaryStage.setScene(scene);
        primaryStage.show();
        // 5. show()
    }

    @Override
    public void stop() {
        // 关闭服务
        wingetService.close();
    }
}