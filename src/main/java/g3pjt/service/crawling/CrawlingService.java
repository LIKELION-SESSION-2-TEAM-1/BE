package g3pjt.service.crawling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlingService {

    private static final String TOUR_KEYWORD_SEARCH_URL =
            "https://apis.data.go.kr/B551011/KorService2/searchKeyword2";

    private static final Pattern FIRST_URL_PATTERN =
            Pattern.compile("(https?://[^\\s\"'>]+)");

    private final ObjectMapper objectMapper;

    @Value("${tour.api.service.key:${TOUR_API_SERVICE_KEY:}}")
    private String tourApiServiceKey;

    public List<StoreDto> searchStoresBatch(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new ArrayList<>();
        }

        List<StoreDto> results = new ArrayList<>();
        for (String keyword : keywords) {
            if (!StringUtils.hasText(keyword)) {
                continue;
            }

            List<StoreDto> one = searchStoresInternal(keyword.trim(), 1);
            if (!one.isEmpty()) {
                results.add(one.get(0));
            }
        }

        return results;
    }

    public List<StoreDto> searchStores(String keyword) {
        return searchStoresInternal(keyword, 10);
    }

    private List<StoreDto> searchStoresInternal(String keyword, int maxItems) {
        if (!StringUtils.hasText(keyword)) return Collections.emptyList();

        if (!StringUtils.hasText(tourApiServiceKey))
            throw new IllegalStateException("Tour API key not configured");

        try {
            return callTourKeywordSearch(keyword, maxItems);
        } catch (Exception e) {
            log.warn("Tour keyword search failed", e);
            throw new IllegalStateException("Tour keyword search failed: " + e.getMessage());
        }
    }

    private List<StoreDto> callTourKeywordSearch(String keyword, int maxItems) throws Exception {

        int numOfRows = Math.min(Math.max(maxItems, 1), 1000);

        // [중요 1] 한글 키워드는 수동으로 인코딩합니다. (강릉 -> %EA%B0%95...)
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

        // [중요 2] ServiceKey 처리
        // properties에 있는 키가 이미 Encoding된 키라면 그대로 쓰고, Decoding된 키라면 인코딩해서 씁니다.
        // 여기서는 안전하게 "있는 그대로" 사용하고 build(true)로 이중 인코딩을 막습니다.
        String serviceKeyToUse = tourApiServiceKey.trim();

        // URI 생성 (build(true) 사용 -> 추가 인코딩 방지)
        URI uri = UriComponentsBuilder.fromHttpUrl(TOUR_KEYWORD_SEARCH_URL)
                .queryParam("serviceKey", serviceKeyToUse)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "MyApp")
                .queryParam("_type", "json")
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", 1)
                .queryParam("arrange", "Q") // Q=이미지순, C=수정일순
                .queryParam("keyword", encodedKeyword)
                .build(true) // [핵심] 이미 인코딩된 값들이니 건드리지 말라고 설정
                .toUri();

        log.info("Generated TourAPI URI: {}", uri); // 로그 확인용

        RestTemplate restTemplate = new RestTemplate();
        String body;

        try {
            body = restTemplate.getForObject(uri, String.class);
        } catch (RestClientResponseException e) {
            String bodySnippet = e.getResponseBodyAsString();
            if (bodySnippet != null && bodySnippet.length() > 500) {
                bodySnippet = bodySnippet.substring(0, 500) + "...";
            }
            // [수정] 컴파일 에러가 나던 변수명을 serviceKeyToUse로 변경하여 해결
            log.warn(
                    "TourAPI HTTP error: status={}, bodySnippet={}, keyword='{}', serviceKeyIsEncoded={}",
                    e.getRawStatusCode(),
                    bodySnippet,
                    keyword,
                    serviceKeyToUse.contains("%")
            );
            throw new IllegalStateException("Tour API error: HTTP " + e.getRawStatusCode() + " (" + e.getStatusText() + ")");
        }

        if (!StringUtils.hasText(body)) return Collections.emptyList();

        // JSON 파싱 시작
        JsonNode root = objectMapper.readTree(body);

        // 에러 응답 체크 (API가 200 OK를 주면서 내부에 에러 코드를 심는 경우 대비)
        JsonNode header = root.path("response").path("header");
        String resultCode = header.path("resultCode").asText("unknown");

        if (!"0000".equals(resultCode) && StringUtils.hasText(resultCode) && !"unknown".equals(resultCode)) {
            String resultMsg = header.path("resultMsg").asText();
            log.error("TourAPI Logic Error: code={}, msg={}, keyword={}", resultCode, resultMsg, keyword);
            return Collections.emptyList();
        }

        JsonNode bodyNode = root.path("response").path("body");
        int totalCount = bodyNode.path("totalCount").asInt(0);
        log.info("Tour keyword search ok: keyword='{}', totalCount={}", keyword, totalCount);

        if (totalCount == 0) return Collections.emptyList();

        JsonNode items = bodyNode.path("items").path("item");
        if (items.isMissingNode() || items.isNull()) return Collections.emptyList();

        List<JsonNode> itemNodes = new ArrayList<>();
        if (items.isArray()) items.forEach(itemNodes::add);
        else itemNodes.add(items);

        List<StoreDto> results = new ArrayList<>();

        for (JsonNode item : itemNodes) {
            String title = item.path("title").asText("").trim();
            if (!StringUtils.hasText(title)) continue;

            String addr1 = item.path("addr1").asText("").trim();
            String addr2 = item.path("addr2").asText("").trim();
            String address = (addr1 + (StringUtils.hasText(addr2) ? (" " + addr2) : "")).trim();

            // 이미지가 없으면 대체 이미지 필드(firstimage2) 확인
            String imageUrl = item.path("firstimage").asText("").trim();
            if (imageUrl.isEmpty()) {
                imageUrl = item.path("firstimage2").asText("").trim();
            }

            String homepage = item.path("homepage").asText("").trim();
            String link = extractFirstUrl(homepage);

            if (!StringUtils.hasText(link)) {
                // 링크 생성 시에도 한글 깨짐 방지를 위해 인코딩 적용
                String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
                link = "https://korean.visitkorea.or.kr/search/search_list.do?keyword=" + encodedTitle;
            }

            StoreDto dto = new StoreDto();
            dto.setStoreName(title);
            dto.setCategory("");
            dto.setAddress(address);
            dto.setRating("0.0");
            dto.setReviewCount("0");
            dto.setLink(link);
            dto.setImageUrl(imageUrl);

            results.add(dto);
            if (results.size() >= maxItems) break;
        }

        return results;
    }

    private String normalizeTourServiceKey(String rawKey) {
        if (!StringUtils.hasText(rawKey)) return "";
        String trimmed = rawKey.trim();
        if (trimmed.contains("%")) return trimmed;
        return UriUtils.encodeQueryParam(trimmed, StandardCharsets.UTF_8);
    }

    private String extractFirstUrl(String text) {
        if (!StringUtils.hasText(text)) return "";
        Matcher m = FIRST_URL_PATTERN.matcher(text);
        if (m.find()) return m.group(1);
        return "";
    }
}