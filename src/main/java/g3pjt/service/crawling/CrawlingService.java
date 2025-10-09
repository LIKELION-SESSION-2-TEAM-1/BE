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

@Service
public class CrawlingService {

    public List<StoreDto> searchStores(String keyword) {
        System.out.println("Crawling started for keyword: " + keyword);

        try {
            String pythonExecutable = "C:\\Users\\antho\\OneDrive\\Desktop\\크롤링\\venv3.12\\Scripts\\python.exe";
            ClassPathResource resource = new ClassPathResource("crawler.py");
            String scriptPath = resource.getFile().getAbsolutePath();

            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, scriptPath, keyword);

            Map<String, String> env = processBuilder.environment();
            env.put("PYTHONIOENCODING", "UTF-8");

            // --- [핵심 수정] 에러 스트림을 별도로 읽기 위해 이 라인을 주석 처리합니다. ---
            // processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // --- [핵심 추가] Python의 에러 메시지(진행 상황 포함)를 읽는 부분 ---
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }

            // Python이 출력한 JSON 문자열 전체를 읽어옴
            String jsonResult;
            try (InputStream inputStream = process.getInputStream()) {
                jsonResult = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();

            // --- [핵심 추가] Python의 에러/진행 메시지를 Java 콘솔에 출력 ---
            if (errorOutput.length() > 0) {
                System.err.println("--- Python Script Stderr Output ---");
                System.err.println(errorOutput.toString());
                System.err.println("-----------------------------------");
            }

            if (exitCode == 0 && jsonResult != null && !jsonResult.trim().isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(jsonResult, new TypeReference<>() {});
            } else {
                System.err.println("Python script exited with error code: " + exitCode + " or produced empty output.");
                return new ArrayList<>();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}