package g3pjt.service.crawling;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Store Crawler API", description = "가게 정보 크롤링 API")
@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final CrawlingService crawlingService;

    public StoreController(CrawlingService crawlingService) {
        this.crawlingService = crawlingService;
    }

    // URL 형식 예시: GET http://localhost:8080/api/stores/search?keyword=강남역%20맛집
    @Operation(summary = "가게 검색 (크롤링)", description = "키워드를 입력받아 실시간으로 가게 정보를 크롤링하여 반환합니다.")
    @GetMapping("/search")
    public ResponseEntity<?> searchStores(
            @Parameter(description = "검색할 키워드 (예: 해운대 맛집)", required = true)
            @RequestParam String keyword
    ) {
        try {
            List<StoreDto> result = crawlingService.searchStores(keyword);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}