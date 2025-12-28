package g3pjt.service.ai.domain;

import g3pjt.service.ai.AiPlanDto;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "user_travel_plans")
public class UserTravelPlan {
    @Id
    private String id;
    
    private Long userId; // The user who saved this plan
    private Long chatRoomId; // The chat room this plan belongs to
    private AiPlanDto plan; // The travel plan details
    
    @CreatedDate
    private LocalDateTime savedAt;
}
