//package g3pjt.service.ai;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import g3pjt.service.chat.domain.ChatDocument;
//import g3pjt.service.chat.repository.ChatRepository;
//import g3pjt.service.ai.domain.AiGeneratedPlan;
//import g3pjt.service.ai.domain.UserTravelPlan;
//import g3pjt.service.ai.repository.AiGeneratedPlanRepository;
//import g3pjt.service.ai.repository.UserTravelPlanRepository;
//import g3pjt.service.crawling.CrawlingService;
//import g3pjt.service.crawling.StoreDto;
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
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class AiService {
//
//    private final ChatRepository chatRepository;
//    private final ObjectMapper objectMapper;
//    private final CrawlingService crawlingService;
//    private final AiGeneratedPlanRepository aiGeneratedPlanRepository;
//    private final UserTravelPlanRepository userTravelPlanRepository;
//
//    @Value("${gemini.api.key}")
//    private String geminiApiKey;
//
//    // Gemini API URL Template
//    private static final String GEMINI_API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s";
//
//    public AiDto extractKeywords(Long chatRoomId) {
//        // 1. Fetch chat history
//        List<ChatDocument> chatHistory = chatRepository.findByChatRoomId(chatRoomId);
//
//        if (chatHistory.isEmpty()) {
//            return new AiDto(Collections.emptyList(), chatRoomId);
//        }
//
//        // 2. Format chat history into a single string
//        String conversation = chatHistory.stream()
//                .map(chat -> chat.getSenderName() + ": " + chat.getMessage())
//                .collect(Collectors.joining("\n"));
//
//        // 3. Call Gemini API
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
//        // System Instruction + User Prompt
//        String systemInstruction = "You are an expert AI geography assistant. Your goal is to extract only valid, real-world geographical locations (cities, countries, provinces, or famous tourist landmarks) from the user's conversation. " +
//                "Strict Rules: " +
//                "1. Verify Existence: Only return locations that can be found on a real map. " +
//                "2. Exclude Noise: Do NOT include slang, verbs, common nouns (e.g., 'gang', 'job', 'food'), typos, or ambiguous words. " +
//                "3. Context: If a word is not a clear destination, ignore it. " +
//                "4. Output Format: Return ONLY a comma-separated list of keywords. If no valid locations are found, return the string 'NONE'. Do not add any other text.";
//
//        String userPrompt = "Chat History:\n" + conversation;
//
//        // Build Payload
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
//        requestBody.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))));
//        requestBody.put("generationConfig", Map.of("temperature", 0.1)); // Low temp for extraction
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        try {
//            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
//            String content = extractGeminiResponse(response.getBody());
//
//            return parseKeywordList(content);
//        } catch (Exception e) {
//            log.error("Error calling Gemini API for keywords", e);
//        }
//
//        return Collections.emptyList();
//    }
//
//    private List<String> parseKeywordList(String content) {
//        if (content == null) {
//            return Collections.emptyList();
//        }
//
//        String normalized = content
//                .replace("```", " ")
//                .replace("\n", ",")
//                .replace("\r", ",")
//                .replace("•", ",")
//                .trim();
//
//        if (normalized.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        String upper = normalized.toUpperCase(Locale.ROOT);
//        if (upper.equals("NONE") || upper.contains("NONE")) {
//            return Collections.emptyList();
//        }
//        if (normalized.contains("없음") || normalized.contains("없습니다") || normalized.contains("없어요")) {
//            return Collections.emptyList();
//        }
//
//        return Arrays.stream(normalized.split(","))
//                .map(String::trim)
//                .filter(s -> !s.isEmpty())
//                .distinct()
//                .collect(Collectors.toList());
//    }
//
//    public AiPlanDto generateTravelPlan(Long chatRoomId, List<String> keywords) {
//        if (keywords == null || keywords.isEmpty()) {
//            return new AiPlanDto(null, "No Plan", "No destinations provided.", Collections.emptyList());
//        }
//
//        // 1. Crawl data for all keywords at once (Batch Processing)
//        List<StoreDto> crawledPlaces = new ArrayList<>();
//
//        // Remove empty keywords
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
//        // 2. Construct prompt with crawled data
//        StringBuilder placesInfo = new StringBuilder();
//        if (crawledPlaces.isEmpty()) {
//            // Fallback if crawling returned nothing
//             for (String keyword : keywords) {
//                placesInfo.append(String.format("- 이름: %s (정보 없음)\n", keyword));
//            }
//        } else {
//            for (StoreDto place : crawledPlaces) {
//                placesInfo.append(String.format("- 이름: %s, 카테고리: %s, 주소: %s, 평점: %s\n",
//                        place.getStoreName(),
//                        place.getCategory() != null ? place.getCategory() : "미정",
//                        place.getAddress() != null ? place.getAddress() : "미정",
//                        place.getRating() != null ? place.getRating() : "0.0"));
//            }
//        }
//
//        String prompt = "다음 여행지 정보를 바탕으로 최적의 여행 계획을 짜줘:\n" + placesInfo.toString() +
//                "\n이 장소들을 효율적인 동선으로 배치해줘. " +
//                "\n " +
//                "결과는 반드시 다음 JSON 형식으로만 반환해 (Markdown code block 없이, 순수 JSON 텍스트만):\n" +
//                "{ \"title\": \"...\", \"description\": \"...\", \"schedule\": [ { \"day\": 1, \"places\": [ { \"name\": \"...\", \"category\": \"...\", \"address\": \"...\", \"distanceToNext\": \"...\" } ] } ] } " +
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
//        requestBody.put("generationConfig", Map.of("temperature", 0.7));
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        try {
//            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
//            String content = extractGeminiResponse(response.getBody());
//
//            if (content != null && !content.trim().isEmpty()) {
//                // Clean up markdown code blocks if Gemini adds them despite instructions
//                content = content.replace("```json", "").replace("```", "").trim();
//                return objectMapper.readValue(content, AiPlanDto.class);
//            }
//        } catch (Exception e) {
//            log.error("Error calling Gemini API for plan generation", e);
//        }
//
//        return new AiPlanDto(null, "Error", "Failed to generate plan.", Collections.emptyList());
//    }
//
//    private String extractGeminiResponse(String responseBody) {
//        if (responseBody == null || responseBody.isBlank()) {
//            return null;
//        }
//
//        try {
//            JsonNode root = objectMapper.readTree(responseBody);
//            // Gemini path: candidates[0].content.parts[0].text
//            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
//            if (textNode.isMissingNode() || textNode.isNull()) {
//                return null;
//            }
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
//        // Verify ownership
//        if (!existingPlan.getUserId().equals(userId)) {
//            throw new IllegalArgumentException("Unauthorized: You do not own this travel plan.");
//        }
//
//        // Keep chatRoomId consistent even if FE omits it in update payload
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
//        // updated time logic could be added here if needed
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

package g3pjt.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import g3pjt.service.chat.domain.ChatDocument;
import g3pjt.service.chat.domain.ChatRoom; // [Mongo] 채팅방 도메인
import g3pjt.service.chat.repository.ChatRepository;
import g3pjt.service.chat.repository.ChatRoomRepository; // [Mongo] 채팅방 리포지토리
import g3pjt.service.ai.domain.AiGeneratedPlan;
import g3pjt.service.ai.domain.UserTravelPlan;
import g3pjt.service.ai.repository.AiGeneratedPlanRepository;
import g3pjt.service.ai.repository.UserTravelPlanRepository;
import g3pjt.service.crawling.CrawlingService;
import g3pjt.service.crawling.StoreDto;

// [User 관련 Import - PostgreSQL]
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatRepository chatRepository;       // Mongo (대화 내용)
    private final ChatRoomRepository chatRoomRepository; // Mongo (방 정보 & 멤버 ID 목록)
    private final UserRepository userRepository;       // Postgres (유저 상세 정보)

    private final ObjectMapper objectMapper;
    private final CrawlingService crawlingService;
    private final AiGeneratedPlanRepository aiGeneratedPlanRepository;
    private final UserTravelPlanRepository userTravelPlanRepository;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s";

    /**
     * 1. 키워드 추출 (Extract Keywords)
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

        String systemInstruction = "You are an expert AI geography assistant. Your goal is to extract only valid, real-world geographical locations (cities, countries, provinces, or famous tourist landmarks) from the user's conversation. " +
                "Strict Rules: " +
                "1. Verify Existence: Only return locations that can be found on a real map. " +
                "2. Exclude Noise: Do NOT include slang, verbs, common nouns (e.g., 'gang', 'job', 'food'), typos, or ambiguous words. " +
                "3. Context: If a word is not a clear destination, ignore it. " +
                "4. Output Format: Return ONLY a comma-separated list of keywords. If no valid locations are found, return the string 'NONE'. Do not add any other text.";

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
        }

        return Collections.emptyList();
    }

    private List<String> parseKeywordList(String content) {
        if (content == null) return Collections.emptyList();
        String normalized = content.replace("```", " ").replace("\n", ",").replace("\r", ",").replace("•", ",").trim();
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
     * 2. 여행 계획 생성 (Generate Travel Plan) - DB 연결 로직 수정 완료
     */
    public AiPlanDto generateTravelPlan(Long chatRoomId, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new AiPlanDto(null, "No Plan", "No destinations provided.", Collections.emptyList());
        }

        // === [Step 1] MongoDB에서 채팅방 정보 조회 ===
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(chatRoomId);
        List<User> participants = new ArrayList<>();

        if (chatRoom != null) {
            // === [Step 2] 채팅방에 저장된 memberIds(Long List) 추출 ===
            List<Long> memberIds = chatRoom.getMemberIds();

            // === [Step 3] PostgreSQL에서 해당 ID들을 가진 User 정보 일괄 조회 ===
            if (memberIds != null && !memberIds.isEmpty()) {
                participants = userRepository.findByIdIn(memberIds);
            }
        } else {
            log.warn("ChatRoom not found for id: {}", chatRoomId);
        }

        if (participants.isEmpty()) {
            log.warn("No participants found via DB bridge for chatRoomId: {}. Proceeding with default settings.", chatRoomId);
        }

        // 4. 그룹 데이터 집계 (다수결 및 합집합 로직 적용)
        String groupProfileContext = aggregateGroupProfile(participants);

        // 5. 크롤링 진행
        List<StoreDto> crawledPlaces = new ArrayList<>();
        List<String> validKeywords = keywords.stream()
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .collect(Collectors.toList());

        try {
            if (!validKeywords.isEmpty()) {
                crawledPlaces = crawlingService.searchStoresBatch(validKeywords);
            }
        } catch (Exception e) {
            log.error("Failed to crawl for keywords", e);
        }

        // 6. 여행지 정보 텍스트 포맷팅
        StringBuilder placesInfo = new StringBuilder();
        if (crawledPlaces.isEmpty()) {
            for (String keyword : keywords) {
                placesInfo.append(String.format("- 이름: %s (상세 정보 없음)\n", keyword));
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

        // 7. 프롬프트 구성 (그룹 프로필 + 여행지 정보)
        String prompt = groupProfileContext +
                "\n위 [여행 그룹 프로필]과 아래 여행지 리스트를 바탕으로, 우리 그룹을 위한 최적의 여행 계획을 짜줘.\n" +
                "\n[여행지 리스트]:\n" +
                placesInfo.toString() +
                "\n\n[필수 반영 규칙]\n" +
                "1. [CRITICAL_RESTRICTIONS](절대 금지 음식)에 포함된 재료/메뉴가 있는 식당은 동선에서 **무조건 제외**하거나, 확실한 대안이 없다면 방문하지 마.\n" +
                "2. [Majority Pace](다수결 여행 페이스)를 기준으로 일정의 밀도를 정해줘. (느림: 여유롭게, 빠름: 빡빡하게)\n" +
                "3. [Majority Rhythm](다수결 하루 리듬)에 맞춰 하루 시작 시간을 정해줘. (아침형: 8~9시 시작, 야행성: 11시 이후 시작)\n" +
                "4. [Food Preferences](음식 선호)는 '다수결 선호'를 우선시하되, 소수 인원의 의견도 한두 끼니 정도 반영해서 모두가 만족하게 해줘.\n" +
                "5. 장소들은 가장 효율적인 이동 동선(거리 순)으로 배치해야 해.\n" +
                "\n" +
                "결과는 반드시 다음 JSON 형식으로만 반환해 (Markdown code block 없이, 순수 JSON 텍스트만):\n" +
                "{ \"title\": \"...\", \"description\": \"...\", \"schedule\": [ { \"day\": 1, \"places\": [ { \"name\": \"...\", \"category\": \"...\", \"address\": \"...\", \"distanceToNext\": \"...\" } ] } ] } " +
                "\nJSON은 유효해야 하며, 한국어로 작성해줘.";

        AiPlanDto plan = callGeminiToGeneratePlan(prompt);

        // Save Generated Plan
        if (plan != null) {
            AiGeneratedPlan generatedPlan = AiGeneratedPlan.builder()
                    .chatRoomId(chatRoomId)
                    .keywords(validKeywords)
                    .plan(plan)
                    .createdAt(LocalDateTime.now())
                    .build();
            aiGeneratedPlanRepository.save(generatedPlan);
        }

        return plan;
    }

    /**
     * 다수 참여자의 정보를 집계하여 그룹 프로필 문자열 생성
     */
    private String aggregateGroupProfile(List<User> participants) {
        if (participants == null || participants.isEmpty()) {
            return "=== [여행 그룹 프로필] ===\n참여자 정보 없음 (일반적인 기준으로 추천 요망)\n====================\n";
        }

        int totalMembers = participants.size();

        // 1. 여행 페이스 다수결 (빈도수 계산)
        Map<String, Long> paceCounts = participants.stream()
                .map(u -> u.getTravelPace() != null ? u.getTravelPace() : "보통")
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        String majorityPace = getMajorityKey(paceCounts, "보통");

        // 2. 하루 리듬 다수결
        Map<String, Long> rhythmCounts = participants.stream()
                .map(u -> u.getDailyRhythm() != null ? u.getDailyRhythm() : "유연")
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        String majorityRhythm = getMajorityKey(rhythmCounts, "유연");

        // 3. 음식 선호도 (모든 키워드 수집 후 빈도 내림차순 정렬)
        Map<String, Long> foodPrefCounts = participants.stream()
                .filter(u -> u.getFoodPreferences() != null)
                .flatMap(u -> u.getFoodPreferences().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // 선호도 문자열 생성 (예: 한식(3명), 일식(1명))
        String foodPreferencesStr = foodPrefCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> e.getKey() + "(" + e.getValue() + "명)")
                .collect(Collectors.joining(", "));
        if (foodPreferencesStr.isEmpty()) foodPreferencesStr = "특별한 선호 없음";

        // 4. 음식 제약 사항 (합집합 - 단 한 명이라도 못 먹으면 리스트에 추가)
        Set<String> allRestrictions = participants.stream()
                .filter(u -> u.getFoodRestrictions() != null)
                .flatMap(u -> u.getFoodRestrictions().stream())
                .collect(Collectors.toSet());

        String restrictionStr = allRestrictions.isEmpty() ? "없음" : String.join(", ", allRestrictions);

        return String.format(
                "=== [여행 그룹 프로필] ===\n" +
                        "- 총 인원: %d명\n" +
                        "- [Majority Pace] 여행 페이스 (다수결): %s\n" +
                        "- [Majority Rhythm] 하루 리듬 (다수결): %s\n" +
                        "- [Food Preferences] 음식 선호 분포: %s (다수 의견 우선, 소수 의견도 고려)\n" +
                        "- [CRITICAL_RESTRICTIONS] 절대 금지 음식 (전체 합집합): %s (이 재료는 무조건 피할 것)\n" +
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

    private AiPlanDto callGeminiToGeneratePlan(String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        String url = String.format(GEMINI_API_URL_TEMPLATE, geminiApiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("systemInstruction", Map.of("parts", List.of(Map.of("text", "You are a travel expert. Generate a structured travel plan in JSON format. return JSON WITHOUT markdown formatting."))));
        requestBody.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))));
        requestBody.put("generationConfig", Map.of("temperature", 0.7));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String content = extractGeminiResponse(response.getBody());

            if (content != null && !content.trim().isEmpty()) {
                content = content.replace("```json", "").replace("```", "").trim();
                return objectMapper.readValue(content, AiPlanDto.class);
            }
        } catch (Exception e) {
            log.error("Error calling Gemini API for plan generation", e);
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
            log.error("Failed to parse Gemini response", e);
            return null;
        }
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
}