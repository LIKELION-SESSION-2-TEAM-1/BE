package g3pjt.service.Ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiPlanDto {
    private String title;
    private String description;
    private List<DayPlan> schedule;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DayPlan {
        private int day;
        private List<Place> places;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Place {
        private String name;
        private String category;
        private String address;
        private String distanceToNext;
    }
}
