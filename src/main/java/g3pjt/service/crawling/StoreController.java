package g3pjt.service.crawling;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final CrawlingService crawlingService;

    public StoreController(CrawlingService crawlingService) {
        this.crawlingService = crawlingService;
    }

    // URL 형식 예시: GET http://localhost:8080/api/stores/search?keyword=강남역%20맛집
    @GetMapping("/search")
    public List<StoreDto> searchStores(@RequestParam String keyword) {
        // 서비스의 searchStores 메서드를 호출하여 실시간으로 크롤링 실행
        return crawlingService.searchStores(keyword);
    }
}