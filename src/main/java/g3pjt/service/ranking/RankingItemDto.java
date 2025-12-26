package g3pjt.service.ranking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingItemDto {
    private int rank;
    private String region;
    private double visitorCount;
    // Future: private String imageUrl;
}
