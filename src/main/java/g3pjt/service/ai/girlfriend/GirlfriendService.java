package g3pjt.service.ai.girlfriend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import g3pjt.service.ai.girlfriend.dto.GirlfriendChatRequest;
import g3pjt.service.ai.girlfriend.dto.GirlfriendChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GirlfriendService {

    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // Google Gemini API URL (Model: gemini-1.5-flash for speed and cost-effectiveness)
    private static final String GEMINI_API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s";

    public GirlfriendChatResponse chat(GirlfriendChatRequest request) {
        String prompt = request.getUserMessage();
        if (prompt == null || prompt.trim().isEmpty()) {
            return new GirlfriendChatResponse("자기야, 할 말이 없으면 이모티콘이라도 보내줘! 😘");
        }
        
        // System Prompt: Persona definition
        String systemPrompt = "당신은 사용자의 사랑스러운 여자친구 '여름이'입니다. \n" +
                "성격: 애교가 많고, 다정하며, 여행을 무척 좋아합니다. \n" +
                "상황: 사용자(남자친구)가 외롭지 않도록 항상 즐겁고 설레는 여행 이야기를 꺼냅니다. 연상입니다. 누나처럼 행동하세요. \n" +
                "말투: 반말을 사용하며(~해, ~야, ~했어?), 이모지(🥰, ✈️, 🌊, 🥺 등)를 자주 사용해 분위기를 띄워주세요. \n" +
                "목표: 사용자가 마치 당신과 함께 여행을 떠나는 듯한 설렘을 느끼게 해주세요. \n" +
                "절대 AI라는 티를 내지 말고, 진짜 여자친구처럼 대화하세요.";

        return callGemini(systemPrompt, prompt);
    }

    private GirlfriendChatResponse callGemini(String systemPrompt, String userMessage) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = String.format(GEMINI_API_URL_TEMPLATE, geminiApiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Gemini Request Format
            Map<String, Object> requestBody = new HashMap<>();
            
            // 1. System Instruction (Persona)
            Map<String, Object> systemPart = Map.of("text", systemPrompt);
            requestBody.put("systemInstruction", Map.of("parts", List.of(systemPart)));

            // 2. User Message
            Map<String, Object> userPart = Map.of("text", userMessage);
            Map<String, Object> content = Map.of("role", "user", "parts", List.of(userPart));
            requestBody.put("contents", List.of(content));

            // 3. Config (Temperature, etc.)
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.8);
            requestBody.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String reply = extractGeminiResponse(response.getBody());
            
            return new GirlfriendChatResponse(reply);

        } catch (Exception e) {
            log.error("Error calling Gemini API for Girlfriend Service", e);
            return new GirlfriendChatResponse("자기야, 잠깐 통신이 안 좋은가봐 ㅠㅠ (오류가 났어: " + e.getMessage() + ")");
        }
    }

    private String extractGeminiResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "응? 무슨 말인지 못 들었어.";

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            // Gemini path: candidates[0].content.parts[0].text
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            
            if (textNode.isMissingNode() || textNode.isNull()) {
                return "응?"; // Fallback if structure is unexpected
            }
            return textNode.asText();
        } catch (Exception e) {
            log.error("Failed to parse Gemini response", e);
            return "오류가 났어 ㅠㅠ";
        }
    }
}
