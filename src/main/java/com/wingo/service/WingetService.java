package com.wingo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wingo.model.WingetPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WingetService implements AutoCloseable {

    private static final String WINGET = "winget";
    private static final String SOURCE = "--source";

    record CommandResult(int exitCode, String output) {}

    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private CompletableFuture<CommandResult> runCommand(List<String> command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Process process = new ProcessBuilder(command)
                        .redirectErrorStream(true)
                        .start();
                String output = readOutput(process);
                process.waitFor();
                return new CommandResult(process.exitValue(), output);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }, executor);
    }

    private String readOutput(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    public CompletableFuture<Boolean> isAvailableAsync() {
        return runCommand(List.of("winget", "--version"))
                .thenApply(result -> result.exitCode() == 0);
    }

    public CompletableFuture<WingetPackage> showAsync(String id) {
        List<String> cmd = List.of(
                WINGET, "show",
                "--id", id,
                SOURCE, "winget",
                "--output", "json"
        );
        return runCommand(cmd)
                .thenApply(result -> {
                    if (result.exitCode() != 0 || result.output().isBlank()) {
                        return null;
                    }
                    try {
                        return mapper.readValue(result.output(), WingetPackage.class);
                    } catch (IOException e) {
                        return null;
                    }
                });
    }

    public CompletableFuture<List<WingetPackage>> searchAsync(String keyword) {
        List<String> cmd = List.of(
                WINGET, "search", keyword,
                SOURCE, "winget",
                "--output", "json"
        );
        return runCommand(cmd)
                .thenApply(result -> {
                    try {
                        return mapper.readValue(result.output(), new TypeReference<>() {});
                    } catch (IOException e) {
                        return List.of();
                    }
                });
    }

    public CompletableFuture<Integer> installAsync(String id) {
        List<String> cmd = List.of(
                WINGET, "install",
                "--id", id, "-e",
                SOURCE, "winget",
                "--silent",
                "--accept-package-agreements",
                "--accept-source-agreements"
        );
        return runCommand(cmd)
                .thenApply(CommandResult::exitCode);
    }

    @Override
    public void close() {
        executor.close();
    }
}