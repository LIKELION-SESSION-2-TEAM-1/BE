package g3pjt.service.Ai;

import g3pjt.service.chat.ChatDocument;
import g3pjt.service.chat.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatRepository chatRepository;
    
    @Value("${openai.api.key}")
    private String openAiApiKey;

    private final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    public AiDto extractKeywords(Long chatRoomId) {
        // 1. Fetch chat history
        List<ChatDocument> chatHistory = chatRepository.findByChatRoomId(chatRoomId);
        
        if (chatHistory.isEmpty()) {
            return new AiDto(Collections.emptyList());
        }

        // 2. Format chat history into a single string
        String conversation = chatHistory.stream()
                .map(chat -> chat.getSenderName() + ": " + chat.getMessage())
                .collect(Collectors.joining("\n"));

        // 3. Call OpenAI API
        List<String> keywords = callOpenAiToExtractKeywords(conversation);

        return new AiDto(keywords);
    }

    private List<String> callOpenAiToExtractKeywords(String conversation) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        
        List<Map<String, String>> messages = new ArrayList<>();
        // messages.add(Map.of("role", "system", "content", "You are a helpful assistant that analyzes chat conversations to extract travel destination keywords. Extract key locations, cities, or countries mentioned as potential travel destinations. Return ONLY a comma-separated list of keywords. If no travel destinations are found, return an empty string."));
        messages.add(Map.of("role", "system", "content",
                "You are an expert AI geography assistant. Your goal is to extract only valid, real-world geographical locations (cities, countries, provinces, or famous tourist landmarks) from the user's conversation. " +
                        "Strict Rules: " +
                        "1. Verify Existence: Only return locations that can be found on a real map. " +
                        "2. Exclude Noise: Do NOT include slang, verbs, common nouns (e.g., 'gang', 'job', 'food'), typos, or ambiguous words. " +
                        "3. Context: If a word is not a clear destination, ignore it. " +
                        "Return ONLY a comma-separated list of keywords. If no valid locations are found, return an empty string."
        ));
        messages.add(Map.of("role", "user", "content", "Chat History:\n" + conversation));
        
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.5);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(OPENAI_API_URL, entity, Map.class);
            
            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    
                    if (content != null && !content.trim().isEmpty()) {
                        return Arrays.stream(content.split(","))
                                .map(String::trim)
                                .collect(Collectors.toList());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
        }

        return Collections.emptyList();
    }
}
