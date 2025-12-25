package g3pjt.service.chat.repository;

import g3pjt.service.chat.domain.ChatDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ChatRepository extends MongoRepository<ChatDocument, String> {
    List<ChatDocument> findByChatRoomId(Long chatRoomId);

    List<ChatDocument> findByChatRoomIdOrderByTimestampAsc(Long chatRoomId);

    void deleteByChatRoomId(Long chatRoomId);
}
