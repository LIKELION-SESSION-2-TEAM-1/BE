package g3pjt.service.chat.repository;

import g3pjt.service.chat.domain.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    ChatRoom findByRoomId(Long roomId);
    java.util.List<ChatRoom> findByMemberIdsContains(Long userId);
}
