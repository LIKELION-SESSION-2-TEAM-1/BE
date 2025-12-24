package g3pjt.service.chat;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ChatRepository extends MongoRepository<ChatDocument, String> {
    List<ChatDocument> findByChatRoomId(Long chatRoomId);

    List<ChatDocument> findByChatRoomIdOrderByTimestampAsc(Long chatRoomId);
}
