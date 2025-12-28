package g3pjt.service.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiPlanDto {
    @Schema(description = "여행 계획 제목", example = "제주도 힐링 여행")
    private String title;
    
    @Schema(description = "여행 계획 설명", example = "아름다운 해변과 맛집을 탐방하는 코스")
    private String description;
    
    @Schema(description = "일자별 스케줄 리스트")
    private List<DayPlan> schedule;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DayPlan {
        @Schema(description = "여행 일차 (1, 2, ...)", example = "1")
        private int day;
        
        @Schema(description = "해당 일차의 방문 장소 리스트")
        private List<Place> places;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Place {
        @Schema(description = "장소 이름", example = "협재 해수욕장")
        private String name;
        
        @Schema(description = "장소 카테고리", example = "관광지")
        private String category;
        
        @Schema(description = "장소 주소", example = "제주시 한림읍 협재리")
        private String address;
        
        @Schema(description = "다음 장소까지의 거리/이동시간", example = "차량 20분")
        private String distanceToNext;

        @Schema(description = "방문 완료/체크 여부", example = "false")
        private boolean isChecked = false;
    }
}
