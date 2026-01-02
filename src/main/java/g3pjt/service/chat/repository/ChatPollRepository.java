package g3pjt.service.chat.repository;

import g3pjt.service.chat.domain.ChatPollDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatPollRepository extends MongoRepository<ChatPollDocument, String> {
    List<ChatPollDocument> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);
}
