package g3pjt.service.chat.repository;

import g3pjt.service.chat.domain.ChatRoomReadState;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatRoomReadStateRepository extends MongoRepository<ChatRoomReadState, String> {
    Optional<ChatRoomReadState> findByRoomIdAndUserId(Long roomId, Long userId);
}
