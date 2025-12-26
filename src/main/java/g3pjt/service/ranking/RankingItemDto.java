package g3pjt.service.ranking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingItemDto {
    @Schema(description = "순위", example = "1")
    private int rank;

    @Schema(description = "지역명 (기초/광역)", example = "제주특별자치도")
    private String region;

    @Schema(description = "방문객 수 합계 (외지인+외국인)", example = "123456.7")
    private double visitorCount;
    // Future: private String imageUrl;
}
