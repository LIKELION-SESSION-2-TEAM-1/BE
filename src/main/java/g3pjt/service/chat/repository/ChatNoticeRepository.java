package g3pjt.service.chat.repository;

import g3pjt.service.chat.domain.ChatNoticeDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatNoticeRepository extends MongoRepository<ChatNoticeDocument, String> {
    List<ChatNoticeDocument> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);
}
