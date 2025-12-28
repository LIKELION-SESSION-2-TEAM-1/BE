package g3pjt.service.ai.repository;

import g3pjt.service.ai.domain.AiGeneratedPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiGeneratedPlanRepository extends MongoRepository<AiGeneratedPlan, String> {
    List<AiGeneratedPlan> findByChatRoomId(Long chatRoomId);
}
