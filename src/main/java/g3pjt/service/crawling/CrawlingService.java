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

        String serviceKeyParam = normalizeTourServiceKey(tourApiServiceKey);
        String encodedKeyword = UriUtils.encodeQueryParam(keyword, StandardCharsets.UTF_8);

        // build(true) 사용: serviceKey/keyword를 우리가 미리 인코딩해서 넘긴다.
        String uri = UriComponentsBuilder.fromHttpUrl(TOUR_KEYWORD_SEARCH_URL)
                .queryParam("serviceKey", serviceKeyParam)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "MyApp")
                .queryParam("_type", "json")
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", 1)
                .queryParam("arrange", "C")
            .queryParam("keyword", encodedKeyword)
                .build(true)
                .toUriString();

        RestTemplate restTemplate = new RestTemplate();
        String body;

        try {
            body = restTemplate.getForObject(uri, String.class);
        } catch (RestClientResponseException e) {
            String bodySnippet = e.getResponseBodyAsString();
            if (bodySnippet != null && bodySnippet.length() > 500) {
                bodySnippet = bodySnippet.substring(0, 500) + "...";
            }
            log.warn(
                    "TourAPI HTTP error: status={}, bodySnippet={}, keyword='{}', serviceKeyEncoded={}",
                    e.getRawStatusCode(),
                    bodySnippet,
                    keyword,
                    serviceKeyParam.contains("%")
            );
            throw new IllegalStateException("Tour API error: HTTP " + e.getRawStatusCode() + " (" + e.getStatusText() + ")");
        }

        if (!StringUtils.hasText(body)) return Collections.emptyList();

        JsonNode root = objectMapper.readTree(body);
        JsonNode response = root.path("response");
        JsonNode bodyNode = response.path("body");

        int totalCount = bodyNode.path("totalCount").asInt(-1);
        log.info("Tour keyword search ok: keyword='{}', totalCount={}", keyword, totalCount);

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

            String imageUrl = item.path("firstimage").asText("").trim();
            String homepage = item.path("homepage").asText("").trim();

            String link = extractFirstUrl(homepage);
            if (!StringUtils.hasText(link)) {

                String encodedTitle = UriComponentsBuilder.newInstance()
                        .queryParam("k", title)
                        .build()
                        .getQueryParams()
                        .getFirst("k");

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

        // 디코딩 키(+, /, =) 포함 가능. query param 기준으로 인코딩.
        return UriUtils.encodeQueryParam(trimmed, StandardCharsets.UTF_8);
    }

    private String extractFirstUrl(String text) {
        if (!StringUtils.hasText(text)) return "";
        Matcher m = FIRST_URL_PATTERN.matcher(text);
        if (m.find()) return m.group(1);
        return "";
    }
}
