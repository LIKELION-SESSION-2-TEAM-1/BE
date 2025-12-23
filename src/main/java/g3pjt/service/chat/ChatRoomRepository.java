package g3pjt.service.chat;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    ChatRoom findByRoomId(Long roomId);
    java.util.List<ChatRoom> findByMemberIdsContains(Long userId);
}
