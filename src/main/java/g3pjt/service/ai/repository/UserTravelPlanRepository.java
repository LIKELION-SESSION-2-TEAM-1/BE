package g3pjt.service.ai.repository;

import g3pjt.service.ai.domain.UserTravelPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTravelPlanRepository extends MongoRepository<UserTravelPlan, String> {
    List<UserTravelPlan> findByUserId(Long userId);

    List<UserTravelPlan> findByUserIdAndChatRoomId(Long userId, Long chatRoomId);
}
