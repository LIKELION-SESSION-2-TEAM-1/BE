package g3pjt.service.ranking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {

    private final ObjectMapper objectMapper;

    // Use the key provided via environment variable or properties
    @Value("${tour.datalab.service.key}")
    private String dataLabServiceKey;

    private static final String LOCGO_API_URL = "https://apis.data.go.kr/B551011/DataLabService/locgoRegnVisitrDDList";
    private static final String METCO_API_URL = "https://apis.data.go.kr/B551011/DataLabService/metcoRegnVisitrDDList";

    public List<RankingItemDto> getWeeklyRanking() {
        // 1. 시도: 최근 데이터 (2일 전)
        LocalDate targetDate = LocalDate.now().minusDays(2);
        List<RankingItemDto> result = tryFetchBoth(targetDate);

        // 2. 시도: 데이터가 없다면 1주 전 데이터 시도
        if (result.isEmpty()) {
            targetDate = LocalDate.now().minusWeeks(1);
            log.info("Recent data not found. Trying 1 week ago: {}", targetDate);
            result = tryFetchBoth(targetDate);
        }

        // 3. 시도: 그래도 없다면 안전한 과거 데이터(20240115)로 고정 (데모용)
        if (result.isEmpty()) {
            // 과거 특정 날짜는 LocalDate 파싱이 번거로우니 문자열로 바로 처리하는 헬퍼 사용
            log.info("Still no data. Fallback to safe date: 20240115");
            result = fetchRankingCombined("20240115");
        }

        return result;
    }

    private List<RankingItemDto> tryFetchBoth(LocalDate date) {
        String ymd = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        return fetchRankingCombined(ymd);
    }

    private List<RankingItemDto> fetchRankingCombined(String ymd) {
        Map<String, Double> mergedMap = new HashMap<>();

        // 1. 기초지자체 (Locgo)
        fetchDataToMap(LOCGO_API_URL, ymd, "signguNm", mergedMap);

        // 2. 광역지자체 (Metco)
        fetchDataToMap(METCO_API_URL, ymd, "areaNm", mergedMap);

        if (mergedMap.isEmpty()) return Collections.emptyList();

        // 3. 정렬 및 DTO 변환 (Top 10)
        return mergedMap.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(10)
                .map(entry -> RankingItemDto.builder()
                        .rank(0) // 순위는 나중에 할당하거나 클라이언트가 인덱스로 처리 (여기선 일단 0)
                        .region(entry.getKey())
                        .visitorCount(entry.getValue())
                        .build())
                .peek(dto -> dto.setRank(mergedMap.size())) // Stream 내에서 rank 매기기 힘드므로 아래 for문으로 재정비
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).setRank(i + 1);
                    }
                    return list;
                }));
    }

    private void fetchDataToMap(String apiUrl, String ymd, String regionKey, Map<String, Double> map) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("serviceKey", dataLabServiceKey)
                    .queryParam("numOfRows", 500)
                    .queryParam("pageNo", 1)
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", "AppTest")
                    .queryParam("_type", "json")
                    .queryParam("startYmd", ymd)
                    .queryParam("endYmd", ymd)
                    .build()
                    .toUri();

            // log.info("Fetching from {}: {}", apiUrl, uri); 
            String response = restTemplate.getForObject(uri, String.class);
            if (response == null) return;

            JsonNode root = objectMapper.readTree(response);
            JsonNode itemsNode = root.path("response").path("body").path("items").path("item");

            if (itemsNode.isMissingNode() || itemsNode.isNull()) return;

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    processItem(item, regionKey, map);
                }
            } else {
                processItem(itemsNode, regionKey, map);
            }

        } catch (Exception e) {
            log.warn("Failed to fetch/parse data from {}: {}", apiUrl, e.getMessage());
        }
    }

    private void processItem(JsonNode item, String regionKey, Map<String, Double> map) {
        String region = item.path(regionKey).asText("").trim();
        if (region.isEmpty()) return;

        // 1: 현지인 제외
        String divCd = item.path("touDivCd").asText();
        if ("1".equals(divCd)) return;

        double count = item.path("touNum").asDouble(0.0);
        map.merge(region, count, Double::sum);
    }
}
