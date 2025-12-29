package g3pjt.service.ai.domain;

import g3pjt.service.ai.AiPlanDto;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Document(collection = "ai_generated_plans")
public class AiGeneratedPlan {
    @Id
    private String id;
    
    private Long chatRoomId;
    private List<String> keywords;
    private AiPlanDto plan;
    
    @CreatedDate
    private LocalDateTime createdAt;
}
