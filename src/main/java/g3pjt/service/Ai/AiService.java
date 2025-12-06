package g3pjt.service.Ai;

import g3pjt.service.chat.ChatDocument;
import g3pjt.service.chat.ChatRepository;
import g3pjt.service.crawling.CrawlingService;
import g3pjt.service.crawling.StoreDto;
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
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatRepository chatRepository;
    private final ObjectMapper objectMapper;
    private final CrawlingService crawlingService;
    
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

    public AiPlanDto generateTravelPlan(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new AiPlanDto("No Plan", "No destinations provided.", Collections.emptyList());
        }

        // 1. Crawl data for all keywords at once (Batch Processing)
        List<StoreDto> crawledPlaces = new ArrayList<>();
        try {
            // Remove empty keywords
            List<String> validKeywords = keywords.stream()
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .collect(Collectors.toList());

            if (!validKeywords.isEmpty()) {
                crawledPlaces = crawlingService.searchStoresBatch(validKeywords);
            }
        } catch (Exception e) {
            log.error("Failed to crawl for keywords", e);
        }

        // 2. Construct prompt with crawled data
        StringBuilder placesInfo = new StringBuilder();
        if (crawledPlaces.isEmpty()) {
            // Fallback if crawling returned nothing
             for (String keyword : keywords) {
                placesInfo.append(String.format("- 이름: %s (정보 없음)\n", keyword));
            }
        } else {
            for (StoreDto place : crawledPlaces) {
                placesInfo.append(String.format("- 이름: %s, 카테고리: %s, 주소: %s, 평점: %s\n",
                        place.getStoreName(), 
                        place.getCategory() != null ? place.getCategory() : "미정", 
                        place.getAddress() != null ? place.getAddress() : "미정", 
                        place.getRating() != null ? place.getRating() : "0.0"));
            }
        }

        String prompt = "다음 여행지 정보를 바탕으로 최적의 여행 계획을 짜줘:\n" + placesInfo.toString() +
                "\n이 장소들을 효율적인 동선으로 배치해줘. " +
                "결과는 반드시 다음 JSON 형식으로만 반환해 (다른 텍스트 없이):\n" +
                "{ \"title\": \"...\", \"description\": \"...\", \"schedule\": [ { \"day\": 1, \"places\": [ { \"name\": \"...\", \"category\": \"...\", \"address\": \"...\", \"distanceToNext\": \"...\" } ] } ] } " +
                "\nJSON은 유효해야 하며, 한국어로 작성해줘.";

        return callOpenAiToGeneratePlan(prompt);
    }

    private AiPlanDto callOpenAiToGeneratePlan(String prompt) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "You are a travel expert. Generate a structured travel plan in JSON format. 반드시 한국어로"));
        messages.add(Map.of("role", "user", "content", prompt));

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(OPENAI_API_URL, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");

                    if (content != null && !content.trim().isEmpty()) {
                        // Clean up markdown code blocks if present
                        content = content.replace("```json", "").replace("```", "").trim();
                        return objectMapper.readValue(content, AiPlanDto.class);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error calling OpenAI API for plan generation", e);
        }

        return new AiPlanDto("Error", "Failed to generate plan.", Collections.emptyList());
    }
}
