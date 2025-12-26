package g3pjt.service.ranking;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ranking")
@Tag(name = "Ranking API", description = "인기 여행지 랭킹 (관광데이터랩 기반)")
public class RankingController {

    private final RankingService rankingService;

    @Operation(summary = "주간 인기 여행지 랭킹 조회", description = "기초지자체(시/군/구) 및 광역지자체 방문자 데이터를 통합하여 방문객 수(현지인 제외) 기준 Top 10을 반환합니다.")
    @GetMapping("/weekly")
    public List<RankingItemDto> getWeeklyRanking() {
        return rankingService.getWeeklyRanking();
    }
}
