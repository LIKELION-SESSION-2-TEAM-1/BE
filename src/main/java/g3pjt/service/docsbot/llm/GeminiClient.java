package g3pjt.service.docsbot.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${docsbot.gemini.model:gemini-2.0-flash}")
    private String model;

    private static final String GEMINI_API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    public String generate(String systemPrompt, String userPrompt, double temperature) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = String.format(GEMINI_API_URL_TEMPLATE, model, geminiApiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();

            // system
            Map<String, Object> systemPart = Map.of("text", systemPrompt);
            requestBody.put("systemInstruction", Map.of("parts", List.of(systemPart)));

            // user
            Map<String, Object> userPart = Map.of("text", userPrompt);
            Map<String, Object> content = Map.of("role", "user", "parts", List.of(userPart));
            requestBody.put("contents", List.of(content));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", temperature);
            requestBody.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            return extractText(response.getBody());
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new IllegalStateException("Gemini API 호출 실패: " + e.getMessage(), e);
        }
    }

    private String extractText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "";
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.isNull()) return "";
            return textNode.asText();
        } catch (Exception e) {
            log.error("Failed to parse Gemini response", e);
            return "";
        }
    }
}
