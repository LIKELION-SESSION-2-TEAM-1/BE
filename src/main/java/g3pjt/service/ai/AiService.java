////package g3pjt.service.ai;
////
////import com.fasterxml.jackson.databind.JsonNode;
////import com.fasterxml.jackson.databind.ObjectMapper;
////import g3pjt.service.chat.domain.ChatDocument;
////import g3pjt.service.chat.repository.ChatRepository;
////import g3pjt.service.ai.domain.AiGeneratedPlan;
////import g3pjt.service.ai.domain.UserTravelPlan;
////import g3pjt.service.ai.repository.AiGeneratedPlanRepository;
////import g3pjt.service.ai.repository.UserTravelPlanRepository;
////import g3pjt.service.crawling.CrawlingService;
////import g3pjt.service.crawling.StoreDto;
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.beans.factory.annotation.Value;
////import org.springframework.http.HttpEntity;
////import org.springframework.http.HttpHeaders;
////import org.springframework.http.MediaType;
////import org.springframework.http.ResponseEntity;
////import org.springframework.stereotype.Service;
////import org.springframework.web.client.RestTemplate;
////
////import java.time.LocalDateTime;
////import java.util.*;
////import java.util.stream.Collectors;
////
////@Slf4j
////@Service
////@RequiredArgsConstructor
////public class AiService {
////
////    private final ChatRepository chatRepository;
////    private final ObjectMapper objectMapper;
////    private final CrawlingService crawlingService;
////    private final AiGeneratedPlanRepository aiGeneratedPlanRepository;
////    private final UserTravelPlanRepository userTravelPlanRepository;
////
////    @Value("${gemini.api.key}")
////    private String geminiApiKey;
////
////    // Gemini API URL Template
////    private static final String GEMINI_API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s";
////
////    public AiDto extractKeywords(Long chatRoomId) {
////        // 1. Fetch chat history
////        List<ChatDocument> chatHistory = chatRepository.findByChatRoomId(chatRoomId);
////
////        if (chatHistory.isEmpty()) {
////            return new AiDto(Collections.emptyList(), chatRoomId);
////        }
////
////        // 2. Format chat history into a single string
////        String conversation = chatHistory.stream()
////                .map(chat -> chat.getSenderName() + ": " + chat.getMessage())
////                .collect(Collectors.joining("\n"));
////
////        // 3. Call Gemini API
////        List<String> keywords = callGeminiToExtractKeywords(conversation);
////
////        return new AiDto(keywords, chatRoomId);
////    }
////
////    private List<String> callGeminiToExtractKeywords(String conversation) {
////        RestTemplate restTemplate = new RestTemplate();
////        String url = String.format(GEMINI_API_URL_TEMPLATE, geminiApiKey);
////
////        HttpHeaders headers = new HttpHeaders();
////        headers.setContentType(MediaType.APPLICATION_JSON);
////
////        // System Instruction + User Prompt
////        String systemInstruction = "You are an expert AI geography assistant. Your goal is to extract only valid, real-world geographical locations (cities, countries, provinces, or famous tourist landmarks) from the user's conversation. " +
////                "Strict Rules: " +
////                "1. Verify Existence: Only return locations that can be found on a real map. " +
////                "2. Exclude Noise: Do NOT include slang, verbs, common nouns (e.g., 'gang', 'job', 'food'), typos, or ambiguous words. " +
////                "3. Context: If a word is not a clear destination, ignore it. " +
////                "4. Output Format: Return ONLY a comma-separated list of keywords. If no valid locations are found, return the string 'NONE'. Do not add any other text.";
////
////        String userPrompt = "Chat History:\n" + conversation;
////
////        // Build Payload
////        Map<String, Object> requestBody = new HashMap<>();
////        requestBody.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
////        requestBody.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))));
////        requestBody.put("generationConfig", Map.of("temperature", 0.1)); // Low temp for extraction
////
////        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
////
////        try {
////            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
////            String content = extractGeminiResponse(response.getBody());
////
////            return parseKeywordList(content);
////        } catch (Exception e) {
////            log.error("Error calling Gemini API for keywords", e);
////        }
////
////        return Collections.emptyList();
////    }
////
////    private List<String> parseKeywordList(String content) {
////        if (content == null) {
////            return Collections.emptyList();
////        }
////
////        String normalized = content
////                .replace("```", " ")
////                .replace("\n", ",")
////                .replace("\r", ",")
////                .replace("•", ",")
////                .trim();
////
////        if (normalized.isEmpty()) {
////            return Collections.emptyList();
////        }
////
////        String upper = normalized.toUpperCase(Locale.ROOT);
////        if (upper.equals("NONE") || upper.contains("NONE")) {
////            return Collections.emptyList();
////        }
////        if (normalized.contains("없음") || normalized.contains("없습니다") || normalized.contains("없어요")) {
////            return Collections.emptyList();
////        }
////
////        return Arrays.stream(normalized.split(","))
////                .map(String::trim)
////                .filter(s -> !s.isEmpty())
////                .distinct()
////                .collect(Collectors.toList());
////    }
////
////    public AiPlanDto generateTravelPlan(Long chatRoomId, List<String> keywords) {
////        if (keywords == null || keywords.isEmpty()) {
////            return new AiPlanDto(null, "No Plan", "No destinations provided.", Collections.emptyList());
////        }
////
////        // 1. Crawl data for all keywords at once (Batch Processing)
////        List<StoreDto> crawledPlaces = new ArrayList<>();
////
////        // Remove empty keywords
////        List<String> validKeywords = keywords.stream()
////                .map(String::trim)
////                .filter(k -> !k.isEmpty())
////                .collect(Collectors.toList());
////
////        try {
////            if (!validKeywords.isEmpty()) {
////                crawledPlaces = crawlingService.searchStoresBatch(validKeywords);
////            }
////        } catch (Exception e) {
////            log.error("Failed to crawl for keywords", e);
////        }
////
////        // 2. Construct prompt with crawled data
////        StringBuilder placesInfo = new StringBuilder();
////        if (crawledPlaces.isEmpty()) {
////            // Fallback if crawling returned nothing
////             for (String keyword : keywords) {
////                placesInfo.append(String.format("- 이름: %s (정보 없음)\n", keyword));
////            }
////        } else {
////            for (StoreDto place : crawledPlaces) {
////                placesInfo.append(String.format("- 이름: %s, 카테고리: %s, 주소: %s, 평점: %s\n",
////                        place.getStoreName(),
////                        place.getCategory() != null ? place.getCategory() : "미정",
////                        place.getAddress() != null ? place.getAddress() : "미정",
////                        place.getRating() != null ? place.getRating() : "0.0"));
////            }
////        }
////
////        String prompt = "다음 여행지 정보를 바탕으로 최적의 여행 계획을 짜줘:\n" + placesInfo.toString() +
////                "\n이 장소들을 효율적인 동선으로 배치해줘. " +
////                "\n " +
////                "결과는 반드시 다음 JSON 형식으로만 반환해 (Markdown code block 없이, 순수 JSON 텍스트만):\n" +
////                "{ \"title\": \"...\", \"description\": \"...\", \"schedule\": [ { \"day\": 1, \"places\": [ { \"name\": \"...\", \"category\": \"...\", \"address\": \"...\", \"distanceToNext\": \"...\" } ] } ] } " +
////                "\nJSON은 유효해야 하며, 한국어로 작성해줘.";
////
////        AiPlanDto plan = callGeminiToGeneratePlan(prompt);
////
////        // Save Generated Plan
////        if (plan != null) {
////            AiGeneratedPlan generatedPlan = AiGeneratedPlan.builder()
////                    .chatRoomId(chatRoomId)
////                    .keywords(validKeywords)
////                    .plan(plan)
////                    .createdAt(LocalDateTime.now())
////                    .build();
////            aiGeneratedPlanRepository.save(generatedPlan);
////        }
////
////        return plan;
////    }
////
////    private AiPlanDto callGeminiToGeneratePlan(String prompt) {
////        RestTemplate restTemplate = new RestTemplate();
////        String url = String.format(GEMINI_API_URL_TEMPLATE, geminiApiKey);
////
////        HttpHeaders headers = new HttpHeaders();
////        headers.setContentType(MediaType.APPLICATION_JSON);
////
////        Map<String, Object> requestBody = new HashMap<>();
////        requestBody.put("systemInstruction", Map.of("parts", List.of(Map.of("text", "You are a travel expert. Generate a structured travel plan in JSON format. return JSON WITHOUT markdown formatting."))));
////        requestBody.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))));
////        requestBody.put("generationConfig", Map.of("temperature", 0.7));
////
////        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
////
////        try {
////            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
////            String content = extractGeminiResponse(response.getBody());
////
////            if (content != null && !content.trim().isEmpty()) {
////                // Clean up markdown code blocks if Gemini adds them despite instructions
////                content = content.replace("```json", "").replace("```", "").trim();
////                return objectMapper.readValue(content, AiPlanDto.class);
////            }
////        } catch (Exception e) {
////            log.error("Error calling Gemini API for plan generation", e);
////        }
////
////        return new AiPlanDto(null, "Error", "Failed to generate plan.", Collections.emptyList());
////    }
////
////    private String extractGeminiResponse(String responseBody) {
////        if (responseBody == null || responseBody.isBlank()) {
////            return null;
////        }
////
////        try {
////            JsonNode root = objectMapper.readTree(responseBody);
////            // Gemini path: candidates[0].content.parts[0].text
////            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
////            if (textNode.isMissingNode() || textNode.isNull()) {
////                return null;
////            }
////            return textNode.asText();
////        } catch (Exception e) {
////            log.error("Failed to parse Gemini response", e);
////            return null;
////        }
////    }
////
////    public UserTravelPlan confirmPlan(Long userId, AiPlanDto finalPlan) {
////        UserTravelPlan userPlan = UserTravelPlan.builder()
////                .userId(userId)
////                .chatRoomId(finalPlan != null ? finalPlan.getChatRoomId() : null)
////                .plan(finalPlan)
////                .savedAt(LocalDateTime.now())
////                .build();
////        return userTravelPlanRepository.save(userPlan);
////    }
////
////    public UserTravelPlan updateTravelPlan(String planId, Long userId, AiPlanDto updatedPlan) {
////        UserTravelPlan existingPlan = userTravelPlanRepository.findById(planId)
////                .orElseThrow(() -> new IllegalArgumentException("Travel plan not found with id: " + planId));
////
////        // Verify ownership
////        if (!existingPlan.getUserId().equals(userId)) {
////            throw new IllegalArgumentException("Unauthorized: You do not own this travel plan.");
////        }
////
////        // Keep chatRoomId consistent even if FE omits it in update payload
////        if (updatedPlan != null) {
////            if (updatedPlan.getChatRoomId() == null && existingPlan.getChatRoomId() != null) {
////                updatedPlan.setChatRoomId(existingPlan.getChatRoomId());
////            }
////            if (updatedPlan.getChatRoomId() != null) {
////                existingPlan.setChatRoomId(updatedPlan.getChatRoomId());
////            }
////        }
////
////        existingPlan.setPlan(updatedPlan);
////        // updated time logic could be added here if needed
////        return userTravelPlanRepository.save(existingPlan);
////    }
////
////    public List<UserTravelPlan> getUserPlans(Long userId) {
////        return userTravelPlanRepository.findByUserId(userId);
////    }
////
////    public List<UserTravelPlan> getUserPlans(Long userId, Long chatRoomId) {
////        if (chatRoomId == null) {
////            return getUserPlans(userId);
////        }
////        return userTravelPlanRepository.findByUserIdAndChatRoomId(userId, chatRoomId);
////    }
////
////    public void deletePlan(String planId, Long userId) {
////        UserTravelPlan existingPlan = userTravelPlanRepository.findById(planId)
////                .orElseThrow(() -> new IllegalArgumentException("Travel plan not found with id: " + planId));
////
////        if (!existingPlan.getUserId().equals(userId)) {
////            throw new IllegalArgumentException("Unauthorized: You do not own this travel plan.");
////        }
////
////        userTravelPlanRepository.delete(existingPlan);
////    }
////}
//
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

package g3pjt.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import g3pjt.service.chat.domain.ChatDocument;
import g3pjt.service.chat.domain.ChatRoom; // ChatRoom 도메인
import g3pjt.service.chat.repository.ChatRepository;
import g3pjt.service.chat.repository.ChatRoomRepository; // ChatRoom 레포지토리
import g3pjt.service.ai.domain.AiGeneratedPlan;
import g3pjt.service.ai.domain.UserTravelPlan;
import g3pjt.service.ai.repository.AiGeneratedPlanRepository;
import g3pjt.service.ai.repository.UserTravelPlanRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository; // [추가] 채팅방 정보 조회용
    private final ObjectMapper objectMapper;
    private final CrawlingService crawlingService;
    private final AiGeneratedPlanRepository aiGeneratedPlanRepository;
    private final UserTravelPlanRepository userTravelPlanRepository;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s";

    /**
     * 1. 대화 내역에서 여행 키워드(장소) 추출
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

    /**
     * 2. 키워드 및 채팅방 설정(날짜, 스타일)을 기반으로 여행 계획 생성
     */
    public AiPlanDto generateTravelPlan(Long chatRoomId, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new AiPlanDto(null, "No Plan", "No destinations provided.", Collections.emptyList());
        }

        // [Step 1] 채팅방 정보(날짜, 스타일) 조회 (스크린샷 기반 수정: findByRoomId 사용)
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(chatRoomId);

        String tripScheduleInfo = "일정 미정";
        String tripStyleInfo = "스타일 정보 없음";
        long totalDays = 1;

        if (chatRoom != null) {
            // 날짜 계산 (ChatRoom 엔티티에 getStartDate, getEndDate 존재 가정)
            LocalDate startDate = chatRoom.getStartDate();
            LocalDate endDate = chatRoom.getEndDate();

            if (startDate != null && endDate != null) {
                totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                tripScheduleInfo = String.format("%s ~ %s (%d박 %d일)", startDate, endDate, totalDays - 1, totalDays);
            } else if (startDate != null) {
                tripScheduleInfo = String.format("%s (당일치기)", startDate);
            }

            // 스타일 정보 병합
            if (chatRoom.getStyles() != null && !chatRoom.getStyles().isEmpty()) {
                tripStyleInfo = String.join(", ", chatRoom.getStyles());
            }
        } else {
            log.warn("ChatRoom not found for id: {}", chatRoomId);
        }

        // [Step 2] 키워드 기반 장소 크롤링
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

        // [Step 3] 프롬프트 데이터 구성 (크롤링 결과)
        StringBuilder placesInfo = new StringBuilder();
        if (crawledPlaces.isEmpty()) {
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

        // [Step 4] 프롬프트 생성 (날짜, 스타일 정보 주입)
        String prompt = "당신은 전문 여행 플래너입니다. 아래 정보를 바탕으로 완벽한 여행 계획을 세워주세요.\n\n" +
                "[여행 기본 정보]\n" +
                "- 여행 기간: " + tripScheduleInfo + "\n" +
                "- 여행 스타일: " + tripStyleInfo + "\n\n" +
                "[후보 장소 리스트 (AI 검색 결과)]\n" +
                placesInfo.toString() + "\n" +
                "[요청 사항]\n" +
                "1. 위 여행 기간(" + totalDays + "일)에 맞춰서 일자별(Day 1, Day 2...) 상세 일정을 계획해줘.\n" +
                "2. 여행 스타일(" + tripStyleInfo + ")을 고려하여 장소를 배치해줘. (예: 힐링이면 여유롭게, 액티비티면 동적으로)\n" +
                "3. 후보 장소들을 효율적인 동선으로 배치하고, 만약 일수가 부족하면 후보 장소 외에 주변 추천 장소를 적절히 추가해도 좋아.\n" +
                "4. 결과는 반드시 다음 JSON 형식으로만 반환해 (Markdown code block 없이, 순수 JSON 텍스트만):\n" +
                "{ \n" +
                "  \"title\": \"여행 제목\", \n" +
                "  \"description\": \"전반적인 여행 컨셉 요약\", \n" +
                "  \"schedule\": [ \n" +
                "    { \n" +
                "      \"day\": 1, \n" +
                "      \"places\": [ \n" +
                "        { \"name\": \"장소명\", \"category\": \"카테고리\", \"address\": \"주소\", \"distanceToNext\": \"다음장소까지 거리\" } \n" +
                "      ] \n" +
                "    } \n" +
                "  ] \n" +
                "} \n" +
                "JSON은 유효해야 하며, 한국어로 작성해줘.";

        // [Step 5] Gemini 호출 및 저장
        AiPlanDto plan = callGeminiToGeneratePlan(prompt);

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

    // --- Gemini API 호출 로직 (키워드 추출) ---
    private List<String> callGeminiToExtractKeywords(String conversation) {
        RestTemplate restTemplate = new RestTemplate();
        String url = String.format(GEMINI_API_URL_TEMPLATE, geminiApiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemInstruction = "You are an expert AI geography assistant. Extract valid real-world locations from the conversation. " +
                "Return ONLY a comma-separated list. If none, return 'NONE'.";

        String userPrompt = "Chat History:\n" + conversation;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
        requestBody.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))));
        requestBody.put("generationConfig", Map.of("temperature", 0.1));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return parseKeywordList(extractGeminiResponse(response.getBody()));
        } catch (Exception e) {
            log.error("Error calling Gemini API for keywords", e);
        }
        return Collections.emptyList();
    }

    // --- Gemini API 호출 로직 (플랜 생성) ---
    private AiPlanDto callGeminiToGeneratePlan(String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        String url = String.format(GEMINI_API_URL_TEMPLATE, geminiApiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("systemInstruction", Map.of("parts", List.of(Map.of("text", "You are a travel expert. Return JSON WITHOUT markdown formatting."))));
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

    // --- 유틸리티 메서드 ---
    private List<String> parseKeywordList(String content) {
        if (content == null) return Collections.emptyList();
        String normalized = content.replace("```", " ").replace("\n", ",").replace("\r", ",").replace("•", ",").trim();
        if (normalized.isEmpty() || normalized.toUpperCase().contains("NONE")) return Collections.emptyList();

        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private String extractGeminiResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            return (textNode.isMissingNode() || textNode.isNull()) ? null : textNode.asText();
        } catch (Exception e) {
            log.error("Failed to parse Gemini response", e);
            return null;
        }
    }

    // --- DB 저장/조회 메서드 ---
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
                .orElseThrow(() -> new IllegalArgumentException("Travel plan not found: " + planId));
        if (!existingPlan.getUserId().equals(userId)) throw new IllegalArgumentException("Unauthorized");

        if (updatedPlan != null) {
            if (updatedPlan.getChatRoomId() == null) updatedPlan.setChatRoomId(existingPlan.getChatRoomId());
            existingPlan.setChatRoomId(updatedPlan.getChatRoomId());
        }
        existingPlan.setPlan(updatedPlan);
        return userTravelPlanRepository.save(existingPlan);
    }

    public List<UserTravelPlan> getUserPlans(Long userId) {
        return userTravelPlanRepository.findByUserId(userId);
    }

    public List<UserTravelPlan> getUserPlans(Long userId, Long chatRoomId) {
        return chatRoomId == null ? getUserPlans(userId) : userTravelPlanRepository.findByUserIdAndChatRoomId(userId, chatRoomId);
    }

    public void deletePlan(String planId, Long userId) {
        UserTravelPlan existingPlan = userTravelPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));
        if (!existingPlan.getUserId().equals(userId)) throw new IllegalArgumentException("Unauthorized");
        userTravelPlanRepository.delete(existingPlan);
    }
}