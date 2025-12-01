package g3pjt.service.crawling;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.io.File;
import java.io.IOException;

@Service
public class CrawlingService {

    @org.springframework.beans.factory.annotation.Value("${python.executable.path:python}")
    private String configuredPythonPath;

    public List<StoreDto> searchStoresBatch(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new ArrayList<>();
        }

        System.out.println("Batch crawling started for keywords: " + keywords);

        try {
            ClassPathResource resource = new ClassPathResource("crawler.py");
            String scriptPath = resource.getFile().getAbsolutePath();
            String pythonExecutable = resolvePythonExecutable();

            // Build command: python script.py keyword1 keyword2 ...
            List<String> command = new ArrayList<>();
            command.add(pythonExecutable);
            command.add(scriptPath);
            command.addAll(keywords);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Map<String, String> env = processBuilder.environment();
            env.put("PYTHONIOENCODING", "UTF-8");

            Process process = processBuilder.start();

            // Read output
            String jsonResult;
            try (InputStream inputStream = process.getInputStream()) {
                jsonResult = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();

            if (exitCode == 0 && jsonResult != null && !jsonResult.trim().isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(jsonResult, new TypeReference<>() {});
            } else {
                return new ArrayList<>();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<StoreDto> searchStores(String keyword) {
        // Fallback or single use
        return searchStoresBatch(List.of(keyword));
    }

    private String resolvePythonExecutable() {
        // 1. Try configured path (from application.properties or env var)
        if (isValidPython(configuredPythonPath)) {
            return configuredPythonPath;
        }

        // 2. Try "python" (System PATH)
        if (isValidPython("python")) {
            return "python";
        }

        // 3. Try "python3" (Mac/Linux)
        if (isValidPython("python3")) {
            return "python3";
        }

        // 4. Fallback: User's specific local path (Last resort for local dev)
        String localFallback = "C:\\Users\\choke\\AppData\\Local\\Programs\\Python\\Python312\\python.exe";
        if (new File(localFallback).exists()) {
            return localFallback;
        }

        // Default to configured path even if validation failed, to show meaningful error later
        return configuredPythonPath;
    }

    private boolean isValidPython(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder(path, "--version");
            pb.start().waitFor();
            return true;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}