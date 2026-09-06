package com.wingo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ConfigService {

    private static final Logger LOGGER = LogManager.getLogger(ConfigService.class);

    private final ObjectMapper mapper;
    private final Path configPath;

    public ConfigService() throws IOException {
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);

        String base = System.getenv("LOCALAPPDATA");
        if (base == null) {
            base = System.getProperty("user.home");
        }
        this.configPath = Paths.get(base, "WinGo", "userconfig.json");
        Files.createDirectories(configPath.getParent());
    }

    private Map<String, String> loadConfig() {
        if (!Files.exists(configPath)) {
            return new HashMap<>();
        }
        try {
            return mapper.readValue(configPath.toFile(),
                    new TypeReference<>() {});
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    public CompletableFuture<Void> saveRememberedChoiceAsync(String appName, String id) {
        return CompletableFuture.runAsync(() -> {
            Map<String, String> map = loadConfig();
            map.put(appName, id);
            try {
                mapper.writeValue(configPath.toFile(), map);
            } catch (IOException e) {
                LOGGER.error("Failed to save user config", e);
            }
        });
    }

    public CompletableFuture<String> getRememberedChoiceAsync(String appName) {
        return CompletableFuture.supplyAsync(() -> loadConfig().get(appName));
    }
}