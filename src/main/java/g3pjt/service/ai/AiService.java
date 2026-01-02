//package g3pjt.service.ai;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import g3pjt.service.chat.domain.ChatDocument;
//import g3pjt.service.chat.domain.ChatRoom; // [Mongo] ChatRoom
//import g3pjt.service.chat.repository.ChatRepository;
//import g3pjt.service.chat.repository.ChatRoomRepository; // [Mongo] ChatRoomRepo
//import g3pjt.service.ai.domain.AiGeneratedPlan;
//import g3pjt.service.ai.domain.UserTravelPlan;
//import g3pjt.service.ai.repository.AiGeneratedPlanRepository;
//import g3pjt.service.ai.repository.UserTravelPlanRepository;
//import g3pjt.service.crawling.CrawlingService;
//import g3pjt.service.crawling.StoreDto;
//
//// [User Import - PostgreSQL]
//import g3pjt.service.user.User;
//import g3pjt.service.user.UserRepository;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.temporal.ChronoUnit;
//import java.util.*;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class AiService {
//
//    private final ChatRepository chatRepository;       // Mongo
//    private final ChatRoomRepository chatRoomRepository; // Mongo
//    private final UserRepository userRepository;       // Postgres
//
//    private final ObjectMapper objectMapper;
//    private final CrawlingService crawlingService;
//    private final AiGeneratedPlanRepository aiGeneratedPlanRepository;
//    private final UserTravelPlanRepository userTravelPlanRepository;
//
//    @Value("${gemini.api.key}")
//    private String geminiApiKey;
//
//    private static final String GEMINI_API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s";
//
//    /**
//     * 1. 키워드 추출 (Extract Keywords)
//     * - 변경사항: 지역명뿐만 아니라 '음식' 키워드도 추출하도록 프롬프트 수정
//     */
//    public AiDto extractKeywords(Long chatRoomId) {
//        List<ChatDocument> chatHistory = chatRepository.findByChatRoomId(chatRoomId);
//
//        if (chatHistory.isEmpty()) {
//            return new AiDto(Collections.emptyList(), chatRoomId);
//        }
//
//        String conversation = chatHistory.stream()
//                .map(chat -> chat.getSenderName() + ": " + chat.getMessage())
//                .collect(Collectors.joining("\n"));
//
//        List<String> keywords = callGeminiToExtractKeywords(conversation);
//
//        return new AiDto(keywords, chatRoomId);
//    }
//
//    private List<String> callGeminiToExtractKeywords(String conversation) {
//        RestTemplate restTemplate = new RestTemplate();
//        String url = String.format(GEMINI_API_URL_TEMPLATE, geminiApiKey);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        // [Prompt 수정] 지역(Locations) + 음식(Food/Dishes) 추출 요청
//        String systemInstruction = "You are an expert AI travel assistant. Your goal is to extract keywords from the conversation to search for travel destinations and restaurants. " +
//                "Strict Rules: " +
//                "1. Extract two types of keywords: " +
//                "   - Geographical locations (cities, landmarks). " +
//                "   - Specific food names or dish categories (e.g., 'Black Pork', 'Sushi', 'Dessert'). " +
//                "2. Verification: Only return words that are valid for a search query on a map or restaurant review site. " +
//                "3. Exclude Noise: Do NOT include verbs, slang, or general words like 'trip', 'fun', 'tomorrow'. " +
//                "4. Output Format: Return ONLY a comma-separated list of keywords. If nothing valid is found, return 'NONE'.";
//
//        String userPrompt = "Chat History:\n" + conversation;
//
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
//        requestBody.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))));
//        requestBody.put("generationConfig", Map.of("temperature", 0.1));
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        try {
//            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
//            String content = extractGeminiResponse(response.getBody());
//            return parseKeywordList(content);
//        } catch (Exception e) {
//            log.error("Error calling Gemini API for keywords", e);
//        }
//
//        return Collections.emptyList();
//    }
//
//    private List<String> parseKeywordList(String content) {
//        if (content == null) return Collections.emptyList();
//        String normalized = content.replace("```", " ").replace("\n", ",").replace("\r", ",").replace("•", ",").trim();
//        if (normalized.isEmpty()) return Collections.emptyList();
//
//        String upper = normalized.toUpperCase(Locale.ROOT);
//        if (upper.equals("NONE") || upper.contains("NONE")) return Collections.emptyList();
//        if (normalized.contains("없음") || normalized.contains("없습니다")) return Collections.emptyList();
//
//        return Arrays.stream(normalized.split(","))
//                .map(String::trim)
//                .filter(s -> !s.isEmpty())
//                .distinct()
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * 2. 여행 계획 생성 (Generate Travel Plan)
//     * - DB 연결 유지
//     * - 날짜 계산 로직 추가
//     * - 필터링(음식 제약, 평점) 프롬프트 강화
//     */
//    public AiPlanDto generateTravelPlan(Long chatRoomId, List<String> keywords) {
//        if (keywords == null || keywords.isEmpty()) {
//            return new AiPlanDto(null, "No Plan", "No destinations provided.", Collections.emptyList());
//        }
//
//        // === [Step 1] MongoDB에서 채팅방 정보 조회 ===
//        ChatRoom chatRoom = chatRoomRepository.findByRoomId(chatRoomId);
//        List<User> participants = new ArrayList<>();
//        String durationInfo = "당일치기"; // 기본값
//
//        if (chatRoom != null) {
//            // A. 참여자 조회 (Mongo ID -> Postgres User)
//            List<Long> memberIds = chatRoom.getMemberIds();
//            if (memberIds != null && !memberIds.isEmpty()) {
//                participants = userRepository.findByIdIn(memberIds);
//            }
//
//            // B. 여행 기간 계산 (StartDate ~ EndDate)
//            if (chatRoom.getStartDate() != null && chatRoom.getEndDate() != null) {
//                long days = ChronoUnit.DAYS.between(chatRoom.getStartDate(), chatRoom.getEndDate()) + 1;
//                durationInfo = String.format("%s ~ %s (%d박 %d일)",
//                        chatRoom.getStartDate(), chatRoom.getEndDate(), days - 1, days);
//            } else if (chatRoom.getStartDate() != null) {
//                durationInfo = chatRoom.getStartDate() + " (당일)";
//            }
//        } else {
//            log.warn("ChatRoom not found for id: {}", chatRoomId);
//        }
//
//        if (participants.isEmpty()) {
//            log.warn("No participants found via DB bridge for chatRoomId: {}. Proceeding with default settings.", chatRoomId);
//        }
//
//        // 3. 그룹 프로필 생성 (제약사항 포함)
//        String groupProfileContext = aggregateGroupProfile(participants);
//
//        // 4. 크롤링 진행 (지역명 + 음식 키워드 모두 검색)
//        List<StoreDto> crawledPlaces = new ArrayList<>();
//        List<String> validKeywords = keywords.stream()
//                .map(String::trim)
//                .filter(k -> !k.isEmpty())
//                .collect(Collectors.toList());
//
//        try {
//            if (!validKeywords.isEmpty()) {
//                crawledPlaces = crawlingService.searchStoresBatch(validKeywords);
//            }
//        } catch (Exception e) {
//            log.error("Failed to crawl for keywords", e);
//        }
//
//        // 5. 여행지/식당 정보 포맷팅 (평점 정보 강조)
//        StringBuilder placesInfo = new StringBuilder();
//        if (crawledPlaces.isEmpty()) {
//            for (String keyword : keywords) {
//                placesInfo.append(String.format("- 키워드: %s (검색 결과 없음)\n", keyword));
//            }
//        } else {
//            for (StoreDto place : crawledPlaces) {
//                placesInfo.append(String.format("- 식당/장소명: %s | 카테고리: %s | 주소: %s | ⭐평점: %s\n",
//                        place.getStoreName(),
//                        place.getCategory() != null ? place.getCategory() : "기타",
//                        place.getAddress() != null ? place.getAddress() : "위치 정보 없음",
//                        place.getRating() != null ? place.getRating() : "0.0"));
//            }
//        }
//
//        // 6. 프롬프트 구성 (기간 + 제약 조건 + 고평점 필터링 요청)
//        String prompt = groupProfileContext +
//                "\n\n[여행 일정 정보]\n" +
//                "- 기간: " + durationInfo + "\n" +
//                "\n[검색된 후보 장소 및 식당 리스트 (평점 포함)]:\n" +
//                placesInfo.toString() +
//                "\n\n[필수 수행 미션]\n" +
//                "위의 [후보 장소 리스트]와 [여행 그룹 프로필]을 분석하여 " + durationInfo + " 일정의 최적의 여행 계획을 짜줘.\n" +
//                "\n[★중요: 장소 선정 및 필터링 규칙★]\n" +
//                "1. **알러지/못 먹는 음식 필터링**: [CRITICAL_RESTRICTIONS]에 적힌 재료나 음식을 파는 식당은 **리스트에서 즉시 제외**해. (예: '해산물' 금지면 횟집, 조개구이집 제외, '고기' 금지면 국밥, 고기집 제외)\n" +
//                "2. **고평점 우선 추천**: 남은 후보지 중에서 '평점'이 높은 곳을 우선적으로 일정에 배치해.\n" +
//                "3. **동선 최적화**: 선택된 장소들을 이동 거리가 짧은 순서대로 배치해.\n" +
//                "4. **키워드 반영**: 사용자가 대화에서 언급한 음식(키워드) 관련 식당이 있다면 우선적으로 포함해.\n" +
//                "5. **여행 일정 정보 필수 반영**: 채팅방 생성 시 적용되었던 여행 일정(몇박 몇일인지, 월과 일을 반영해).\n" +
//                "6. **일정 개수**: 하루 일정이 최소 4개는 되게끔 일정을 배치해줘.\n" +
//                "7. **주소 상세 표기**: 모든 장소의 'address' 필드는 반드시 구글 지도에서 검색 가능한 '전체 도로명 주소'로 정확하게 기입해.\n" +
//                "\n" +
//                "결과는 반드시 다음 JSON 형식으로만 반환해 (Markdown code block 없이, 순수 JSON 텍스트만):\n" +
//                "{ \"title\": \"여행 제목\", \"description\": \"전반적인 컨셉 설명\", \"schedule\": [ { \"day\": 1, \"date\": \"YYYY-MM-DD\", \"places\": [ { \"name\": \"장소명\", \"category\": \"업종\", \"address\": \"주소\", \"distanceToNext\": \"다음 장소까지 거리\" } ] } ] } " +
//                "\nJSON은 유효해야 하며, 한국어로 작성해줘.";
//
//        AiPlanDto plan = callGeminiToGeneratePlan(prompt);
//
//        // Save Generated Plan
//        if (plan != null) {
//            AiGeneratedPlan generatedPlan = AiGeneratedPlan.builder()
//                    .chatRoomId(chatRoomId)
//                    .keywords(validKeywords)
//                    .plan(plan)
//                    .createdAt(LocalDateTime.now())
//                    .build();
//            aiGeneratedPlanRepository.save(generatedPlan);
//        }
//
//        return plan;
//    }
//
//    /**
//     * 다수 참여자의 정보를 집계하여 그룹 프로필 문자열 생성
//     */
//    private String aggregateGroupProfile(List<User> participants) {
//        if (participants == null || participants.isEmpty()) {
//            return "=== [여행 그룹 프로필] ===\n참여자 정보 없음 (일반적인 기준으로 추천 요망)\n====================\n";
//        }
//
//        int totalMembers = participants.size();
//
//        // 1. 여행 페이스
//        Map<String, Long> paceCounts = participants.stream()
//                .map(u -> u.getTravelPace() != null ? u.getTravelPace() : "보통")
//                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
//        String majorityPace = getMajorityKey(paceCounts, "보통");
//
//        // 2. 하루 리듬
//        Map<String, Long> rhythmCounts = participants.stream()
//                .map(u -> u.getDailyRhythm() != null ? u.getDailyRhythm() : "유연")
//                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
//        String majorityRhythm = getMajorityKey(rhythmCounts, "유연");
//
//        // 3. 음식 선호도
//        Map<String, Long> foodPrefCounts = participants.stream()
//                .filter(u -> u.getFoodPreferences() != null)
//                .flatMap(u -> u.getFoodPreferences().stream())
//                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
//
//        String foodPreferencesStr = foodPrefCounts.entrySet().stream()
//                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
//                .map(e -> e.getKey() + "(" + e.getValue() + "명)")
//                .collect(Collectors.joining(", "));
//        if (foodPreferencesStr.isEmpty()) foodPreferencesStr = "특별한 선호 없음";
//
//        // 4. [중요] 음식 제약 사항 (합집합)
//        Set<String> allRestrictions = participants.stream()
//                .filter(u -> u.getFoodRestrictions() != null)
//                .flatMap(u -> u.getFoodRestrictions().stream())
//                .collect(Collectors.toSet());
//
//        String restrictionStr = allRestrictions.isEmpty() ? "없음" : String.join(", ", allRestrictions);
//
//        return String.format(
//                "=== [여행 그룹 프로필] ===\n" +
//                        "- 총 인원: %d명\n" +
//                        "- [Majority Pace] 여행 페이스: %s\n" +
//                        "- [Majority Rhythm] 하루 리듬: %s\n" +
//                        "- [Food Preferences] 선호 음식: %s\n" +
//                        "- [CRITICAL_RESTRICTIONS] 절대 금지 음식: %s (이 재료/음식이 포함된 식당은 절대 추천하지 말 것)\n" +
//                        "========================\n",
//                totalMembers, majorityPace, majorityRhythm, foodPreferencesStr, restrictionStr
//        );
//    }
//
//    private String getMajorityKey(Map<String, Long> counts, String defaultValue) {
//        return counts.entrySet().stream()
//                .max(Map.Entry.comparingByValue())
//                .map(Map.Entry::getKey)
//                .orElse(defaultValue);
//    }
//
//    private AiPlanDto callGeminiToGeneratePlan(String prompt) {
//        RestTemplate restTemplate = new RestTemplate();
//        String url = String.format(GEMINI_API_URL_TEMPLATE, geminiApiKey);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("systemInstruction", Map.of("parts", List.of(Map.of("text", "You are a travel expert. Generate a structured travel plan in JSON format. return JSON WITHOUT markdown formatting."))));
//        requestBody.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))));
//
//        // [RAG 기능 추가] Google Search Grounding
//        // google_search_retrieval -> google_search 로 변경 (API 스펙 변경 반영)
//        Map<String, Object> googleSearchTool = Map.of("google_search", Map.of());
//        requestBody.put("tools", List.of(googleSearchTool));
//
//        requestBody.put("generationConfig", Map.of("temperature", 0.7));
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        try {
//            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
//            String content = extractGeminiResponse(response.getBody());
//
//            log.info("Gemini extracted content: {}", content);
//
//            if (content != null && !content.trim().isEmpty()) {
//                String cleanedContent = content.replace("```json", "").replace("```", "").trim();
//
//                if (cleanedContent.isEmpty()) {
//                    log.warn("Gemini returned empty JSON content. Raw content: {}", content);
//                    return new AiPlanDto(null, "Error", "Empty response from AI", Collections.emptyList());
//                }
//
//                return objectMapper.readValue(cleanedContent, AiPlanDto.class);
//            }
//        } catch (org.springframework.web.client.RestClientResponseException e) {
//            log.error("Gemini API Error: Status={}, Body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
//        } catch (Exception e) {
//            log.error("Error calling Gemini API for plan generation", e);
//        }
//
//        return new AiPlanDto(null, "Error", "Failed to generate plan.", Collections.emptyList());
//    }
//
//    private String extractGeminiResponse(String responseBody) {
//        if (responseBody == null || responseBody.isBlank()) return null;
//        try {
//            JsonNode root = objectMapper.readTree(responseBody);
//            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
//            if (textNode.isMissingNode() || textNode.isNull()) return null;
//            return textNode.asText();
//        } catch (Exception e) {
//            log.error("Failed to parse Gemini response", e);
//            return null;
//        }
//    }
//
//    public UserTravelPlan confirmPlan(Long userId, AiPlanDto finalPlan) {
//        UserTravelPlan userPlan = UserTravelPlan.builder()
//                .userId(userId)
//                .chatRoomId(finalPlan != null ? finalPlan.getChatRoomId() : null)
//                .plan(finalPlan)
//                .savedAt(LocalDateTime.now())
//                .build();
//        return userTravelPlanRepository.save(userPlan);
//    }
//
//    public UserTravelPlan updateTravelPlan(String planId, Long userId, AiPlanDto updatedPlan) {
//        UserTravelPlan existingPlan = userTravelPlanRepository.findById(planId)
//                .orElseThrow(() -> new IllegalArgumentException("Travel plan not found with id: " + planId));
//
//        if (!existingPlan.getUserId().equals(userId)) {
//            throw new IllegalArgumentException("Unauthorized: You do not own this travel plan.");
//        }
//
//        if (updatedPlan != null) {
//            if (updatedPlan.getChatRoomId() == null && existingPlan.getChatRoomId() != null) {
//                updatedPlan.setChatRoomId(existingPlan.getChatRoomId());
//            }
//            if (updatedPlan.getChatRoomId() != null) {
//                existingPlan.setChatRoomId(updatedPlan.getChatRoomId());
//            }
//        }
//
//        existingPlan.setPlan(updatedPlan);
//        return userTravelPlanRepository.save(existingPlan);
//    }
//
//    public List<UserTravelPlan> getUserPlans(Long userId) {
//        return userTravelPlanRepository.findByUserId(userId);
//    }
//
//    public List<UserTravelPlan> getUserPlans(Long userId, Long chatRoomId) {
//        if (chatRoomId == null) {
//            return getUserPlans(userId);
//        }
//        return userTravelPlanRepository.findByUserIdAndChatRoomId(userId, chatRoomId);
//    }
//
//    public void deletePlan(String planId, Long userId) {
//        UserTravelPlan existingPlan = userTravelPlanRepository.findById(planId)
//                .orElseThrow(() -> new IllegalArgumentException("Travel plan not found with id: " + planId));
//
//        if (!existingPlan.getUserId().equals(userId)) {
//            throw new IllegalArgumentException("Unauthorized: You do not own this travel plan.");
//        }
//
//        userTravelPlanRepository.delete(existingPlan);
//    }
//}
//
//

package g3pjt.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import g3pjt.service.ai.domain.AiGeneratedPlan;
import g3pjt.service.ai.domain.UserTravelPlan;
import g3pjt.service.ai.repository.AiGeneratedPlanRepository;
import g3pjt.service.ai.repository.UserTravelPlanRepository;
import g3pjt.service.chat.domain.ChatDocument;
import g3pjt.service.chat.domain.ChatRoom;
import g3pjt.service.chat.repository.ChatRepository;
import g3pjt.service.chat.repository.ChatRoomRepository;
import g3pjt.service.crawling.CrawlingService;
import g3pjt.service.crawling.StoreDto;
import g3pjt.service.user.User;
import g3pjt.service.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatRepository chatRepository;              // Mongo: chats
    private final ChatRoomRepository chatRoomRepository;      // Mongo: chat_rooms
    private final UserRepository userRepository;              // Postgres: users

    private final ObjectMapper objectMapper;
    private final CrawlingService crawlingService;
    private final AiGeneratedPlanRepository aiGeneratedPlanRepository;
    private final UserTravelPlanRepository userTravelPlanRepository;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s";

    private static final int MIN_PLACES_PER_DAY = 4;
    private static final int MAX_RETRY = 1;

    /**
     * 1) 키워드 추출
     */
    public AiDto extractKeywords(Long chatRoomId) {
        List<ChatDocument> chatHistory = chatRepository.findByChatRoomId(chatRoomId);

        if (chatHistory.isEmpty()) {
            return new AiDto(Collections.emptyList(), chatRoomId);
        }

        String conversation = chatHistory.stream()
                .map(chat -> chat.getSenderName() + ": " + chat.getMessage())
                .collect(Collectors.joining("\n"));

        List<String> keywords = callGeminiToExtractKeywords(conversation);

        return new AiDto(keywords, chatRoomId);
    }

    private List<String> callGeminiToExtractKeywords(String conversation) {
        RestTemplate restTemplate = new RestTemplate();
        String url = String.format(GEMINI_API_URL_TEMPLATE, geminiApiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemInstruction =
                "You are an expert AI travel assistant. Your goal is to extract keywords from the conversation to search for travel destinations and restaurants. " +
                        "Strict Rules: " +
                        "1. Extract two types of keywords: " +
                        "   - Geographical locations (cities, landmarks). " +
                        "   - Specific food names or dish categories (e.g., 'Black Pork', 'Sushi', 'Dessert'). " +
                        "2. Verification: Only return words that are valid for a search query on a map or restaurant review site. " +
                        "3. Exclude Noise: Do NOT include verbs, slang, or general words like 'trip', 'fun', 'tomorrow'. " +
                        "4. Output Format: Return ONLY a comma-separated list of keywords. If nothing valid is found, return 'NONE'.";

        String userPrompt = "Chat History:\n" + conversation;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
        requestBody.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))));
        requestBody.put("generationConfig", Map.of("temperature", 0.1));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String content = extractGeminiResponse(response.getBody());
            return parseKeywordList(content);
        } catch (Exception e) {
            log.error("Error calling Gemini API for keywords", e);
            return Collections.emptyList();
        }
    }

    private List<String> parseKeywordList(String content) {
        if (content == null) return Collections.emptyList();

        String normalized = content
                .replace("```", " ")
                .replace("\n", ",")
                .replace("\r", ",")
                .replace("•", ",")
                .trim();

        if (normalized.isEmpty()) return Collections.emptyList();

        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.equals("NONE") || upper.contains("NONE")) return Collections.emptyList();
        if (normalized.contains("없음") || normalized.contains("없습니다")) return Collections.emptyList();

        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 2) 여행 계획 생성
     * - 핵심 수정:
     *   (1) chat_rooms의 startDate/endDate로 allowedDates(정확한 날짜 배열) 계산
     *   (2) travelStyle + styles를 프롬프트에 강제 반영
     *   (3) 결과 schedule 일수/날짜/하루 최소 4개 검증 후 위반 시 1회 재생성
     */
    public AiPlanDto generateTravelPlan(Long chatRoomId, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new AiPlanDto(chatRoomId, "No Plan", "No destinations provided.", Collections.emptyList());
        }

        // 키워드 정리
        List<String> validKeywords = keywords.stream()
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // === Step 1) ChatRoom 조회 ===
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(chatRoomId);

        if (chatRoom == null) {
            log.warn("[AI] ChatRoom not found by roomId={}. duration/style will fallback.", chatRoomId);
        } else {
            log.info("[AI] ChatRoom found. roomId={}, startDate={}, endDate={}, travelStyle={}, stylesSize={}",
                    chatRoom.getRoomId(),
                    chatRoom.getStartDate(),
                    chatRoom.getEndDate(),
                    chatRoom.getTravelStyle(),
                    (chatRoom.getStyles() == null ? 0 : chatRoom.getStyles().size())
            );
        }

        // === Step 2) 참가자(Postgres) 조회 ===
        List<User> participants = new ArrayList<>();
        if (chatRoom != null && chatRoom.getMemberIds() != null && !chatRoom.getMemberIds().isEmpty()) {
            participants = userRepository.findByIdIn(chatRoom.getMemberIds());
        }
        if (participants.isEmpty()) {
            log.warn("[AI] No participants found for chatRoomId={}. Proceed with defaults.", chatRoomId);
        }

        // === Step 3) 그룹 프로필(제약 포함) ===
        String groupProfileContext = aggregateGroupProfile(participants);

        // === Step 4) 기간/스타일 컨텍스트 계산 (강제용 데이터) ===
        TripContext trip = buildTripContext(chatRoom);

        // === Step 5) 크롤링 (키워드 기반) ===
        List<StoreDto> crawledPlaces = new ArrayList<>();
        try {
            if (!validKeywords.isEmpty()) {
                crawledPlaces = crawlingService.searchStoresBatch(validKeywords);
            }
        } catch (Exception e) {
            log.error("[AI] Failed to crawl for keywords={}", validKeywords, e);
        }

        // 후보 리스트 문자열화
        String placesInfo = buildPlacesInfo(validKeywords, crawledPlaces);

        // === Step 6) 프롬프트 생성 ===
        String prompt = buildPlanPrompt(groupProfileContext, trip, placesInfo);

        // === Step 7) 1차 생성 ===
        AiPlanDto plan = callGeminiToGeneratePlan(prompt);

        // chatRoomId 보정 (FE/후속 저장 로직에서 사용)
        if (plan != null) {
            plan.setChatRoomId(chatRoomId);
        }

        // === Step 8) 검증 + 필요시 재시도(1회) ===
        plan = retryOnceIfInvalid(plan, prompt, trip, chatRoomId);

        // === Step 9) 결과 저장 ===
        if (plan != null) {
            try {
                AiGeneratedPlan generatedPlan = AiGeneratedPlan.builder()
                        .chatRoomId(chatRoomId)
                        .keywords(validKeywords)
                        .plan(plan)
                        .createdAt(LocalDateTime.now())
                        .build();
                aiGeneratedPlanRepository.save(generatedPlan);
            } catch (Exception e) {
                log.error("[AI] Failed to save AiGeneratedPlan. chatRoomId={}", chatRoomId, e);
            }
        }

        return plan != null
                ? plan
                : new AiPlanDto(chatRoomId, "Error", "Failed to generate plan.", Collections.emptyList());
    }

    private String buildPlacesInfo(List<String> validKeywords, List<StoreDto> crawledPlaces) {
        StringBuilder sb = new StringBuilder();
        if (crawledPlaces == null || crawledPlaces.isEmpty()) {
            for (String keyword : validKeywords) {
                sb.append(String.format("- 키워드: %s (검색 결과 없음)\n", keyword));
            }
            return sb.toString();
        }

        for (StoreDto place : crawledPlaces) {
            sb.append(String.format(
                    "- 장소명: %s | 카테고리: %s | 주소: %s | ⭐평점: %s\n",
                    safe(place.getStoreName(), "이름없음"),
                    safe(place.getCategory(), "기타"),
                    safe(place.getAddress(), "위치 정보 없음"),
                    safe(place.getRating(), "0.0")
            ));
        }
        return sb.toString();
    }

    private String buildPlanPrompt(String groupProfileContext, TripContext trip, String placesInfo) {
        // 스타일 규칙(매핑)은 필요 시 더 늘리시면 됩니다.
        String styleRules =
                "- travelStyle/styles에 '먹방'이 포함되면: 식당/카페 비중을 전체 places의 60% 이상으로 구성\n" +
                        "- 'activity' 또는 '액티비티/체험'이 포함되면: 하루에 체험/액티비티 장소 최소 1개 포함\n" +
                        "- 'shopping' 또는 '쇼핑'이 포함되면: 쇼핑 가능한 장소(시장/쇼핑몰/편집샵/거리)를 하루 최소 1개 포함\n" +
                        "- '힐링'이 포함되면: 이동 거리 최소화 + 카페/산책/뷰포인트 비중 증가\n";

        // allowedDates 강제 문자열
        String allowedDatesStr = trip.allowedDates.isEmpty()
                ? "[]"
                : trip.allowedDates.stream().map(LocalDate::toString).collect(Collectors.joining(", ", "[", "]"));

        // 핵심: schedule 길이 강제 + 날짜 강제
        return groupProfileContext +
                "\n\n[여행 일정 정보]\n" +
                "- 시작일: " + (trip.startDate == null ? "미정" : trip.startDate) + "\n" +
                "- 종료일: " + (trip.endDate == null ? "미정" : trip.endDate) + "\n" +
                "- 기간표기: " + trip.durationInfo + "\n" +
                "- 총 일수(dayCount): " + trip.dayCount + "\n" +
                "- 총 박수(nightCount): " + trip.nightCount + "\n" +
                "- allowedDates(사용 가능한 날짜 목록): " + allowedDatesStr + "\n" +
                "- travelStyle/styles: " + trip.styleInfo + "\n" +

                "\n[검색된 후보 장소 리스트 (평점 포함)]\n" +
                placesInfo +

                "\n[필수 수행 미션]\n" +
                "1) 위 후보 리스트와 그룹 프로필을 분석해서, allowedDates에 맞춘 최적의 여행 일정을 만들어.\n" +
                "2) 동선을 짧게(거리 최소화) 구성하고, 평점 높은 곳을 우선 배치해.\n" +

                "\n[★중요: 장소 선정 및 필터링 규칙★]\n" +
                "1. [CRITICAL_RESTRICTIONS]에 포함된 음식/재료를 파는 식당은 즉시 제외\n" +
                "2. 평점 높은 곳 우선\n" +
                "3. 동선 최적화(이동 거리 최소)\n" +
                "4. 대화에서 나온 음식 키워드 관련 식당이 있으면 우선 포함\n" +
                "5. 스타일 반영(아래 규칙을 반드시 반영):\n" + styleRules +
                "6. 하루 places 개수는 최소 " + MIN_PLACES_PER_DAY + "개 이상\n" +
                "7. address는 구글지도 검색 가능한 전체 도로명 주소로 기입\n" +

                "\n[★기간 강제 조건(매우 중요)★]\n" +
                "- schedule 배열 길이는 반드시 dayCount(" + trip.dayCount + ")와 정확히 일치해야 함\n" +
                "- schedule[i].day는 1부터 dayCount까지 순서대로 1씩 증가해야 함\n" +
                "- schedule[i].date는 allowedDates 목록 중에서만 사용해야 하며, 가능한 한 i번째 날짜를 사용해야 함\n" +
                "- allowedDates가 비어있으면(날짜 미정) dayCount는 1로 하고 date는 \"\"(빈 문자열)로 두어라\n" +

                "\n[출력 형식]\n" +
                "결과는 반드시 아래 JSON 형식(순수 JSON 텍스트, Markdown 금지)으로만 반환:\n" +
                "{\n" +
                "  \"title\": \"여행 제목\",\n" +
                "  \"description\": \"전반적인 컨셉 설명\",\n" +
                "  \"schedule\": [\n" +
                "    {\n" +
                "      \"day\": 1,\n" +
                "      \"date\": \"YYYY-MM-DD\",\n" +
                "      \"places\": [\n" +
                "        { \"name\": \"장소명\", \"category\": \"업종\", \"address\": \"주소\", \"distanceToNext\": \"다음 장소까지 거리\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "JSON은 유효해야 하며 한국어로 작성해.";
    }

    private TripContext buildTripContext(ChatRoom chatRoom) {
        TripContext ctx = new TripContext();
        ctx.startDate = (chatRoom == null ? null : chatRoom.getStartDate());
        ctx.endDate = (chatRoom == null ? null : chatRoom.getEndDate());
        ctx.styleInfo = buildStyleInfo(chatRoom);

        if (ctx.startDate != null && ctx.endDate != null) {
            long days = ChronoUnit.DAYS.between(ctx.startDate, ctx.endDate) + 1; // inclusive
            if (days < 1) days = 1;

            ctx.dayCount = (int) days;
            ctx.nightCount = Math.max(0, ctx.dayCount - 1);
            ctx.allowedDates = enumerateDates(ctx.startDate, ctx.endDate);

            ctx.durationInfo = String.format("%s ~ %s (%d박 %d일)",
                    ctx.startDate, ctx.endDate, ctx.nightCount, ctx.dayCount);
        } else if (ctx.startDate != null) {
            ctx.dayCount = 1;
            ctx.nightCount = 0;
            ctx.allowedDates = List.of(ctx.startDate);
            ctx.durationInfo = ctx.startDate + " (당일)";
        } else {
            // 날짜가 아예 없으면: dayCount=1로 강제(모델이 멋대로 늘리는 것을 방지)
            ctx.dayCount = 1;
            ctx.nightCount = 0;
            ctx.allowedDates = Collections.emptyList();
            ctx.durationInfo = "날짜 미정(당일치기 처리)";
        }

        return ctx;
    }

    private String buildStyleInfo(ChatRoom chatRoom) {
        if (chatRoom == null) return "없음";

        LinkedHashSet<String> styles = new LinkedHashSet<>();

        if (!isBlank(chatRoom.getTravelStyle())) {
            styles.add(chatRoom.getTravelStyle().trim());
        }

        if (chatRoom.getStyles() != null) {
            for (String s : chatRoom.getStyles()) {
                if (!isBlank(s)) styles.add(s.trim());
            }
        }

        if (styles.isEmpty()) return "없음";
        return String.join(", ", styles);
    }

    private List<LocalDate> enumerateDates(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            dates.add(cur);
            cur = cur.plusDays(1);
        }
        return dates;
    }

    private AiPlanDto retryOnceIfInvalid(AiPlanDto plan, String originalPrompt, TripContext trip, Long chatRoomId) {
        if (plan == null) return null;

        boolean ok = validatePlan(plan, trip);
        if (ok) return plan;

        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            log.warn("[AI] Plan validation failed. Retrying once. chatRoomId={}, dayCount={}, allowedDates={}",
                    chatRoomId, trip.dayCount, trip.allowedDates);

            String repairPrompt =
                    originalPrompt +
                            "\n\n[검증 실패로 인한 재생성 요청]\n" +
                            "- 너의 이전 응답이 '기간 강제 조건' 또는 '하루 places 최소 개수' 조건을 위반했다.\n" +
                            "- 반드시 조건을 모두 만족하는 새 JSON만 반환해.\n";

            AiPlanDto regenerated = callGeminiToGeneratePlan(repairPrompt);
            if (regenerated != null) {
                regenerated.setChatRoomId(chatRoomId);
                if (validatePlan(regenerated, trip)) {
                    return regenerated;
                }
                plan = regenerated; // 그래도 최신으로 교체
            }
        }

        return plan;
    }

    /**
     * DTO 구조가 바뀌어도 컴파일 깨지지 않도록, objectMapper로 JsonNode로 변환 후 검증합니다.
     */
    private boolean validatePlan(AiPlanDto plan, TripContext trip) {
        try {
            JsonNode root = objectMapper.valueToTree(plan);
            JsonNode schedule = root.path("schedule");

            // dayCount 강제
            if (!schedule.isArray()) return false;
            if (schedule.size() != trip.dayCount) return false;

            // 날짜 강제(allowedDates 있을 때만)
            Set<String> allowed = trip.allowedDates.stream()
                    .map(LocalDate::toString)
                    .collect(Collectors.toSet());

            for (int i = 0; i < schedule.size(); i++) {
                JsonNode dayNode = schedule.get(i);

                // day 값이 순서대로인지(가능하면)
                int expectedDay = i + 1;
                if (dayNode.has("day") && dayNode.get("day").isInt()) {
                    if (dayNode.get("day").asInt() != expectedDay) return false;
                }

                // date 값 검증(allowedDates 비어있지 않을 때)
                if (!allowed.isEmpty()) {
                    String date = dayNode.path("date").asText("");
                    if (!allowed.contains(date)) return false;
                }

                // places 최소 개수
                JsonNode places = dayNode.path("places");
                if (!places.isArray() || places.size() < MIN_PLACES_PER_DAY) return false;
            }

            return true;
        } catch (Exception e) {
            log.error("[AI] validatePlan error", e);
            return false;
        }
    }

    private AiPlanDto callGeminiToGeneratePlan(String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        String url = String.format(GEMINI_API_URL_TEMPLATE, geminiApiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(
                "systemInstruction",
                Map.of("parts", List.of(Map.of("text",
                        "You are a travel expert. Generate a structured travel plan in valid JSON format. " +
                                "Return JSON ONLY without markdown and without extra commentary.")))
        );
        requestBody.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))));

        // Google Search Grounding
        Map<String, Object> googleSearchTool = Map.of("google_search", Map.of());
        requestBody.put("tools", List.of(googleSearchTool));

        requestBody.put("generationConfig", Map.of("temperature", 0.7));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String content = extractGeminiResponse(response.getBody());

            log.info("[AI] Gemini raw content: {}", content);

            if (content == null || content.trim().isEmpty()) {
                return new AiPlanDto(null, "Error", "Empty response from AI", Collections.emptyList());
            }

            String cleaned = content.replace("```json", "").replace("```", "").trim();

            // 혹시 JSON 앞뒤로 설명이 붙는 경우 대비: 첫 '{' ~ 마지막 '}'만 잘라 파싱
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1).trim();
            }

            if (cleaned.isEmpty()) {
                log.warn("[AI] Gemini returned empty JSON after cleaning. raw={}", content);
                return new AiPlanDto(null, "Error", "Empty JSON from AI", Collections.emptyList());
            }

            return objectMapper.readValue(cleaned, AiPlanDto.class);

        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("[AI] Gemini API Error: Status={}, Body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[AI] Error calling Gemini API for plan generation", e);
        }

        return new AiPlanDto(null, "Error", "Failed to generate plan.", Collections.emptyList());
    }

    private String extractGeminiResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return null;

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.isNull()) return null;
            return textNode.asText();
        } catch (Exception e) {
            log.error("[AI] Failed to parse Gemini response", e);
            return null;
        }
    }

    /**
     * 다수 참여자의 정보를 집계하여 그룹 프로필 문자열 생성
     */
    private String aggregateGroupProfile(List<User> participants) {
        if (participants == null || participants.isEmpty()) {
            return "=== [여행 그룹 프로필] ===\n참여자 정보 없음 (일반적인 기준으로 추천 요망)\n====================\n";
        }

        int totalMembers = participants.size();

        Map<String, Long> paceCounts = participants.stream()
                .map(u -> u.getTravelPace() != null ? u.getTravelPace() : "보통")
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        String majorityPace = getMajorityKey(paceCounts, "보통");

        Map<String, Long> rhythmCounts = participants.stream()
                .map(u -> u.getDailyRhythm() != null ? u.getDailyRhythm() : "유연")
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        String majorityRhythm = getMajorityKey(rhythmCounts, "유연");

        Map<String, Long> foodPrefCounts = participants.stream()
                .filter(u -> u.getFoodPreferences() != null)
                .flatMap(u -> u.getFoodPreferences().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        String foodPreferencesStr = foodPrefCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> e.getKey() + "(" + e.getValue() + "명)")
                .collect(Collectors.joining(", "));
        if (foodPreferencesStr.isEmpty()) foodPreferencesStr = "특별한 선호 없음";

        Set<String> allRestrictions = participants.stream()
                .filter(u -> u.getFoodRestrictions() != null)
                .flatMap(u -> u.getFoodRestrictions().stream())
                .collect(Collectors.toSet());

        String restrictionStr = allRestrictions.isEmpty() ? "없음" : String.join(", ", allRestrictions);

        return String.format(
                "=== [여행 그룹 프로필] ===\n" +
                        "- 총 인원: %d명\n" +
                        "- [Majority Pace] 여행 페이스: %s\n" +
                        "- [Majority Rhythm] 하루 리듬: %s\n" +
                        "- [Food Preferences] 선호 음식: %s\n" +
                        "- [CRITICAL_RESTRICTIONS] 절대 금지 음식: %s (이 재료/음식이 포함된 식당은 절대 추천하지 말 것)\n" +
                        "========================\n",
                totalMembers, majorityPace, majorityRhythm, foodPreferencesStr, restrictionStr
        );
    }

    private String getMajorityKey(Map<String, Long> counts, String defaultValue) {
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(defaultValue);
    }

    public UserTravelPlan confirmPlan(Long userId, AiPlanDto finalPlan) {
        UserTravelPlan userPlan = UserTravelPlan.builder()
                .userId(userId)
                .chatRoomId(finalPlan != null ? finalPlan.getChatRoomId() : null)
                .plan(finalPlan)
                .savedAt(LocalDateTime.now())
                .build();
        return userTravelPlanRepository.save(userPlan);
    }

    public UserTravelPlan updateTravelPlan(String planId, Long userId, AiPlanDto updatedPlan) {
        UserTravelPlan existingPlan = userTravelPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Travel plan not found with id: " + planId));

        if (!existingPlan.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized: You do not own this travel plan.");
        }

        if (updatedPlan != null) {
            if (updatedPlan.getChatRoomId() == null && existingPlan.getChatRoomId() != null) {
                updatedPlan.setChatRoomId(existingPlan.getChatRoomId());
            }
            if (updatedPlan.getChatRoomId() != null) {
                existingPlan.setChatRoomId(updatedPlan.getChatRoomId());
            }
        }

        existingPlan.setPlan(updatedPlan);
        return userTravelPlanRepository.save(existingPlan);
    }

    public List<UserTravelPlan> getUserPlans(Long userId) {
        return userTravelPlanRepository.findByUserId(userId);
    }

    public List<UserTravelPlan> getUserPlans(Long userId, Long chatRoomId) {
        if (chatRoomId == null) {
            return getUserPlans(userId);
        }
        return userTravelPlanRepository.findByUserIdAndChatRoomId(userId, chatRoomId);
    }

    public void deletePlan(String planId, Long userId) {
        UserTravelPlan existingPlan = userTravelPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Travel plan not found with id: " + planId));

        if (!existingPlan.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized: You do not own this travel plan.");
        }

        userTravelPlanRepository.delete(existingPlan);
    }

    // ======== Utils / Inner ========

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String v, String fallback) {
        return isBlank(v) ? fallback : v;
    }

    private static class TripContext {
        LocalDate startDate;
        LocalDate endDate;
        int dayCount;
        int nightCount;
        List<LocalDate> allowedDates = Collections.emptyList();
        String durationInfo = "당일치기";
        String styleInfo = "없음";
    }
}
